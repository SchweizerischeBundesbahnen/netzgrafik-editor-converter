package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.model.DayTimeInterval;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Identifiable;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Label;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.NetworkGraphic;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Port;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Trainrun;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.TrainrunCategory;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.TrainrunCategoryHaltezeit;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.TrainrunFrequency;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.TrainrunSection;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.TrainrunTimeCategory;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Transition;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.NetworkGraphicValidator;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class NetworkGraphicConverter<T> {

    private static final double SECONDS_PER_MINUTE = 60.;

    private final NetworkGraphicConverterConfig config;
    private final NetworkGraphicSource source;
    private final SupplyBuilder<T> builder;
    private final ConverterSink<T> sink;

    private Map<String, Integer> lineCounter;
    private Lookup lookup;
    private Set<Integer> depots;
    private Set<String> linesWithoutDepot;

    private static String createTransitRouteId(String lineId, RouteDirection direction) {
        return String.format("%s_%s", lineId, direction.name());
    }

    public void run() throws IOException {
        log.info("Converting netzgrafik using source {}, supply builder {} and sink {}",
                source.getClass().getSimpleName(), builder.getClass().getSimpleName(), sink.getClass().getSimpleName());

        NetworkGraphic networkGraphic = new NetworkGraphicValidator(config.getValidationStrategy(),
                config.isUseTrainNamesAsIds(), source.load()).run();

        initialize(networkGraphic);
        addStops();
        addTrains();
        handleLinesWithoutDepot();

        sink.save(builder.build());
    }

    private void initialize(NetworkGraphic network) {
        lineCounter = new HashMap<>();
        lookup = new Lookup(network);
        depots = config.getServiceDayBoundary() == ServiceDayBoundary.DEPOT ? findDepots(network) : Set.of();
        linesWithoutDepot = new TreeSet<>();
    }

    private Set<Integer> findDepots(NetworkGraphic network) {
        Set<Integer> depotLabelIds = network.getLabels()
                .stream()
                .filter(label -> config.getDepotLabel().equals(label.getLabel()))
                .map(Label::getId)
                .collect(Collectors.toSet());

        return network.getNodes()
                .stream()
                .filter(node -> node.getLabelIds().stream().anyMatch(depotLabelIds::contains))
                .map(Node::getId)
                .collect(Collectors.toSet());
    }

    private void addStops() {
        log.info("Adding nodes of network graphic");
        for (Node node : lookup.nodes.values()) {
            log.debug("Adding node {}", node.getBetriebspunktName());

            // The input coordinates originate from a graphical editor which uses a top-left origin system (Y-axis
            // increases downwards). To transform these into a standard Cartesian or geographical coordinate system
            // (bottom-left origin), the Y-coordinate must be inverted.
            // Note: These transformed coordinates serve as a fallback. They are typically superseded by real-world
            // coordinates from the infrastructure repository.
            builder.addStopFacility(node.getBetriebspunktName(), node.getFullName(), node.getPositionX(),
                    -node.getPositionY());
        }
    }

    private void addTrains() {
        log.info("Adding trains of network graphic");

        // setup trainrun section sequence builders; order and align sections
        HashMap<Integer, SectionSequenceBuilder> sequences = new HashMap<>();
        for (TrainrunSection section : lookup.sections.values()) {
            int trainId = section.getTrainrunId();
            SectionSequenceBuilder sequence = sequences.computeIfAbsent(trainId,
                    _ -> new SectionSequenceBuilder(lookup.nodes));
            sequence.add(section);
        }

        // add a transit line with a transit route per direction for each train
        for (Map.Entry<Integer, SectionSequenceBuilder> entry : sequences.entrySet()) {
            Trainrun train = lookup.trains.get(entry.getKey());
            SectionSequenceBuilder sequence = entry.getValue();

            log.debug("Adding train {}", train.getName());
            createAndAddTransitLine(train, sequence.build(train.getDirection()));
        }
    }

    private void createAndAddTransitLine(Trainrun train, EnumMap<RouteDirection, List<TrainrunSection>> sequence) {

        String category = lookup.categories.get(train.getCategoryId()).getShortName();
        String lineId = createTransitLineId(train, sequence.get(RouteDirection.FORWARD), category);
        builder.addTransitLine(lineId, category);

        // add transit route for each direction the train operates in
        sequence.forEach((direction, sections) -> createAndAddTransitRoute(lineId, train, sections, direction));

    }

    private void createAndAddTransitRoute(String lineId, Trainrun train, List<TrainrunSection> sections, RouteDirection direction) {

        // create transit route over all nodes of the sections
        String routeId = createTransitRouteId(lineId, direction);
        String fachCategory = lookup.categories.get(train.getCategoryId()).getFachCategory();
        List<Visit> visits = createVisits(train, sections, fachCategory);
        addTransitRoute(routeId, lineId, visits, 0, visits.size() - 1, fachCategory);

        // trainruns with day time intervals always run full trips, the boundary only applies to the whole service day
        List<DayTimeInterval> timeIntervals = lookup.times.get(train.getTrainrunTimeCategoryId()).getDayTimeIntervals();
        if (timeIntervals.isEmpty() && config.getServiceDayBoundary() != ServiceDayBoundary.FULL) {
            addBoundedDepartures(routeId, lineId, train, sections.getFirst(), visits, fachCategory);
            return;
        }

        // add interval for full day in minutes if no interval is set
        if (timeIntervals.isEmpty()) {
            timeIntervals = List.of(DayTimeInterval.builder()
                    .from((int) (Math.round(config.getServiceDayStart().toSecondOfDay() / SECONDS_PER_MINUTE)))
                    .to(((int) Math.round(config.getServiceDayEnd().toSecondOfDay() / SECONDS_PER_MINUTE)))
                    .build());
        }

        // derive departures in time intervals and add to supply builder
        List<ServiceDayTime> departures = createDepartureTimes(timeIntervals, train, sections.getFirst());
        log.debug("Adding departures to {} at: {}", routeId, departures);
        departures.forEach(departure -> builder.addDeparture(routeId, departure));
    }

    /**
     * Create the visits of the nodes along the sections, with times relative to the departure at the first node.
     */
    private List<Visit> createVisits(Trainrun train, List<TrainrunSection> sections, String fachCategory) {
        List<Visit> visits = new ArrayList<>();
        Node sourceNode = lookup.nodes.get(sections.getFirst().getSourceNodeId());
        visits.add(new Visit(sourceNode, false, DwellTime.fromCategory(sourceNode, fachCategory).negated(),
                Duration.ZERO));

        Duration time = Duration.ZERO;
        for (int i = 0; i < sections.size(); i++) {
            TrainrunSection currentSection = sections.get(i);
            Node targetNode = lookup.nodes.get(currentSection.getTargetNodeId());
            time = time.plusMinutes(Math.round(currentSection.getTravelTime().getTime()));

            if (i == sections.size() - 1) {
                // last stop: dwell time from category
                Duration dwellTime = DwellTime.fromCategory(targetNode, fachCategory);
                visits.add(new Visit(targetNode, false, time, time.plus(dwellTime)));

            } else if (isPass(targetNode, currentSection.getId())) {
                // pass: nonstop at node
                visits.add(new Visit(targetNode, true, time, time));

            } else {
                // stop: dwell time from network graphic
                Duration dwellTime = DwellTime.fromSections(currentSection, sections.get(i + 1));

                // check consistency of dwell time from network graphic against dwell time from category
                DwellTime.warnOnInconsistency(dwellTime, train, targetNode, fachCategory);

                visits.add(new Visit(targetNode, false, time, time.plus(dwellTime)));
                time = time.plus(dwellTime);
            }
        }

        return visits;
    }

    /**
     * Add a transit route from the visit at index from to the visit at index to (inclusive), both must be stops.
     */
    private void addTransitRoute(String routeId, String lineId, List<Visit> visits, int from, int to, String fachCategory) {

        // clipped routes start and end with the dwell time from the category, as the full route does at its termini
        Visit origin = visits.get(from);
        Duration dwellTimeAtOrigin = from == 0 ? origin.dwellTime() : DwellTime.fromCategory(origin.node(),
                fachCategory);
        builder.addTransitRoute(routeId, lineId, origin.stopId(), dwellTimeAtOrigin);

        Duration departure = origin.departure();
        for (int i = from + 1; i <= to; i++) {
            Visit visit = visits.get(i);
            if (visit.pass()) {
                builder.addRoutePass(routeId, visit.stopId());
                continue;
            }

            Duration dwellTime = i == to && to < visits.size() - 1 ? DwellTime.fromCategory(visit.node(),
                    fachCategory) : visit.dwellTime();
            builder.addRouteStop(routeId, visit.stopId(), visit.arrival().minus(departure), dwellTime);
            departure = visit.departure();
        }
    }

    /**
     * Add the departures of the whole service day, with the trips at its start and end clipped to the service day.
     * <p>
     * A clipped trip starts at the first stop departing at or after the start and ends at the last stop arriving at or
     * before the end of the service day.
     */
    private void addBoundedDepartures(String routeId, String lineId, Trainrun train, TrainrunSection firstSection, List<Visit> visits, String fachCategory) {
        final int start = config.getServiceDayStart().toSecondOfDay();
        final int end = config.getServiceDayEnd().toSecondOfDay();
        final int frequency = getFrequency(train);
        final int duration = (int) visits.getLast().arrival().toSeconds();
        final int last = visits.size() - 1;
        final boolean depot = config.getServiceDayBoundary() == ServiceDayBoundary.DEPOT;

        if (depot && visits.stream().noneMatch(this::isLabelledDepot)) {
            linesWithoutDepot.add(lineId);
        }

        Set<Section> boundedSections = new HashSet<>();

        // include the trips already running at the service day start
        for (int departure = firstDepartureAtOrAfter(train, firstSection,
                start - duration + 1); departure < end; departure += frequency) {

            // clip: first stop departing after the start and last stop arriving before the end of the service day
            int from = 0;
            while (from < last && (visits.get(from).pass() || departure + visits.get(from).departure()
                    .toSeconds() < start)) {
                from++;
            }
            int to = last;
            while (to > 0 && (visits.get(to).pass() || departure + visits.get(to).arrival().toSeconds() > end)) {
                to--;
            }

            // depot: shorten to the first depot after the start and the last depot before the end
            if (depot) {
                while (from < to && !isDepot(visits, from)) {
                    from++;
                }
                while (to > from && !isDepot(visits, to)) {
                    to--;
                }
            }

            // skip trips with less than two stops within the service day
            if (from >= to) {
                continue;
            }

            // add a route for each distinct section of the full route
            String boundedRouteId = routeId;
            if (from > 0 || to < last) {
                // the visit indices keep the ids unique, also for sections between stops visited twice
                boundedRouteId = String.format("%s_%s_%s_%d_%d", routeId, visits.get(from).stopId(),
                        visits.get(to).stopId(), from, to);
                if (boundedSections.add(new Section(from, to))) {
                    addTransitRoute(boundedRouteId, lineId, visits, from, to, fachCategory);
                }
            }

            int departureAtOrigin = departure + (int) visits.get(from).departure().toSeconds();
            log.debug("Adding departure to {} at: {}", boundedRouteId, departureAtOrigin);
            builder.addDeparture(boundedRouteId, new ServiceDayTime(departureAtOrigin));
        }
    }

    private boolean isDepot(List<Visit> visits, int index) {
        return index == 0 || index == visits.size() - 1 || isLabelledDepot(visits.get(index));
    }

    private boolean isLabelledDepot(Visit visit) {
        return !visit.pass() && depots.contains(visit.node().getId());
    }

    /**
     * Handle the transit lines without a stop labelled as depot, whose trips fall back to start and end at the line
     * termini, according to the validation strategy.
     */
    private void handleLinesWithoutDepot() {
        if (linesWithoutDepot.isEmpty()) {
            return;
        }

        String message = String.format("No stop labelled '%s' on lines %s", config.getDepotLabel(), linesWithoutDepot);
        switch (config.getValidationStrategy()) {
            case SKIP_VALIDATION -> log.debug(message);
            case FAIL_ON_ISSUES -> throw new IllegalStateException(message);
            default -> log.warn("{}, their trips only start and end at the termini", message);
        }
    }

    private String createTransitLineId(Trainrun train, List<TrainrunSection> sections, String category) {

        // check if option is set to use train name; avoid name if it is empty (optional field in NGE)
        String lineId;
        if (config.isUseTrainNamesAsIds() && !train.getName().isBlank()) {
            lineId = train.getName();
        } else {
            // create id from category with origin and destination, ignore the train name from nge
            lineId = String.format("%s_%s_%s", category,
                    lookup.nodes.get(sections.getFirst().getSourceNodeId()).getBetriebspunktName(),
                    lookup.nodes.get(sections.getLast().getTargetNodeId()).getBetriebspunktName());
        }

        // check if line id is already existing
        int count = lineCounter.getOrDefault(lineId, 0);
        if (count > 0) {
            lineCounter.put(lineId, ++count);
            log.warn("Line with id {} is already existing, adding counter {} to id", lineId, count);
            lineId = String.format("%s_%d", lineId, count);
        } else {
            lineCounter.put(lineId, 1);
        }

        return lineId;
    }

    /**
     * Create departure times in day time intervals.
     **/
    private List<ServiceDayTime> createDepartureTimes(List<DayTimeInterval> timeIntervals, Trainrun train, TrainrunSection firstSection) {
        final int frequency = getFrequency(train);

        List<ServiceDayTime> departures = new ArrayList<>();
        for (DayTimeInterval dti : timeIntervals) {
            int fromTime = (int) Math.round(dti.getFrom() * SECONDS_PER_MINUTE);
            int toTime = (int) Math.round(dti.getTo() * SECONDS_PER_MINUTE);
            for (int departureTime = firstDepartureAtOrAfter(train, firstSection,
                    fromTime); departureTime < toTime; departureTime += frequency) {
                departures.add(new ServiceDayTime(departureTime));
            }
        }

        return departures;
    }

    /**
     * Get the first departure at or after the given time.
     * <p>
     * As in the NGE, the departures repeat with the frequency from the consecutive departure time at the first node,
     * which also defines the hour of trainruns with a frequency above 60 minutes (e.g. odd hours).
     */
    private int firstDepartureAtOrAfter(Trainrun train, TrainrunSection firstSection, int time) {
        int pattern = (int) Math.round(firstSection.getSourceDeparture().getConsecutiveTime() * SECONDS_PER_MINUTE);

        return time + Math.floorMod(pattern - time, getFrequency(train));
    }

    /**
     * Get the frequency of a trainrun in seconds.
     */
    private int getFrequency(Trainrun train) {
        return (int) Math.round(lookup.frequencies.get(train.getFrequencyId()).getFrequency() * SECONDS_PER_MINUTE);
    }

    /**
     * Check if a node is a nonstop transit.
     * <p>
     * Model: Node --> Transition --> Port --> TrainrunSection --> Trainrun
     *
     * @param node              the node to check.
     * @param trainrunSectionId the trainrun section id.
     * @return true if it is a nonstop transit pass.
     */
    private boolean isPass(Node node, int trainrunSectionId) {
        List<Transition> nodeTransitions = node.getTransitions();
        if (nodeTransitions == null) {
            return false;
        }

        for (Transition transition : nodeTransitions) {
            if (!transition.isNonStopTransit()) {
                continue;
            }
            Port port1 = lookup.ports.get(transition.getPort1Id());
            if (port1 != null && port1.getTrainrunSectionId() == trainrunSectionId) {
                return true;
            }
            Port port2 = lookup.ports.get(transition.getPort2Id());
            if (port2 != null && port2.getTrainrunSectionId() == trainrunSectionId) {
                return true;
            }
        }

        return false;
    }

    /**
     * Visit of a node on a transit route, with arrival and departure relative to the departure at the first node.
     */
    private record Visit(Node node, boolean pass, Duration arrival, Duration departure) {

        private String stopId() {
            return node.getBetriebspunktName();
        }

        private Duration dwellTime() {
            return departure.minus(arrival);
        }

    }

    /**
     * Section of a transit route between the visits at index from and to (inclusive).
     */
    private record Section(int from, int to) {
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private static class DwellTime {

        private static final String UNCATEGORIZED = "HaltezeitUncategorized";

        /**
         * Get the dwell time of a train category at a node
         * <p>
         * If the fach category is uncategorized, then the connection time of the node is returned.
         */
        private static Duration fromCategory(Node node, String fachCategory) {

            if (fachCategory.equals(UNCATEGORIZED)) {
                Duration dwellTime = Duration.ofSeconds(Math.round(node.getConnectionTime() * SECONDS_PER_MINUTE));
                log.warn("Uncategorized dwell time category, returning connection time instead ({} s)",
                        dwellTime.toSeconds());
                return dwellTime;
            }

            TrainrunCategoryHaltezeit trainrunCategoryHaltezeit = node.getTrainrunCategoryHaltezeiten()
                    .get(fachCategory);
            if (trainrunCategoryHaltezeit == null) {
                String message = String.format("Invalid fachCategory value %s at node %s.", fachCategory,
                        node.getBetriebspunktName());
                throw new IllegalStateException(message);
            }

            if (trainrunCategoryHaltezeit.isNoHalt()) {
                return Duration.ofSeconds(0);
            }

            return Duration.ofSeconds(Math.round(trainrunCategoryHaltezeit.getHaltezeit() * SECONDS_PER_MINUTE));
        }

        private static Duration fromSections(TrainrunSection currentSection, TrainrunSection nextSection) {
            double arrivalTime = currentSection.getTargetArrival().getTime();
            double departureTime = nextSection.getSourceDeparture().getTime();

            if (arrivalTime > departureTime) {
                // special case: The departure time is in the next hour
                // example: arrivalTime = 59, departureTime = 1; actual dwell time = 2 minutes
                return Duration.ofSeconds(Math.round((departureTime + 60 - arrivalTime) * SECONDS_PER_MINUTE));
            } else {
                // normal case: Arrival time is before departure time within the same hour
                return Duration.ofSeconds(Math.round((departureTime - arrivalTime) * SECONDS_PER_MINUTE));
            }
        }

        private static void warnOnInconsistency(Duration dwellTime, Trainrun train, Node targetNode, String fachCategory) {
            Duration dwellTimeFromCategory = DwellTime.fromCategory(targetNode, fachCategory);
            if (!dwellTime.equals(dwellTimeFromCategory)) {
                log.warn("Dwell time inconsistency: trainrun={}, node={}, category={}, expected={}s, actual={}s.",
                        train.getName(), targetNode.getBetriebspunktName(), fachCategory,
                        dwellTimeFromCategory.toSeconds(), dwellTime.toSeconds());
            }
        }

    }

    /**
     * Keep track of the NG elements, allows lookups by ids.
     */
    private static class Lookup {

        private final Map<Integer, Node> nodes;
        private final Map<Integer, Port> ports;
        private final Map<Integer, Trainrun> trains;
        private final Map<Integer, TrainrunSection> sections;
        private final Map<Integer, TrainrunCategory> categories;
        private final Map<Integer, TrainrunFrequency> frequencies;
        private final Map<Integer, TrainrunTimeCategory> times;

        Lookup(NetworkGraphic network) {
            this.nodes = listToHashMap(network.getNodes());
            this.ports = listToHashMap(
                    network.getNodes().stream().map(Node::getPorts).flatMap(List::stream).collect(Collectors.toList()));
            this.trains = listToHashMap(network.getTrainruns());
            this.sections = listToHashMap(network.getTrainrunSections());
            this.categories = listToHashMap(network.getMetadata().getTrainrunCategories());
            this.frequencies = listToHashMap(network.getMetadata().getTrainrunFrequencies());
            this.times = listToHashMap(network.getMetadata().getTrainrunTimeCategories());
        }

        private <T extends Identifiable> Map<Integer, T> listToHashMap(List<T> list) {
            return list.stream()
                    .collect(Collectors.toMap(Identifiable::getId, element -> element, (element, _) -> element,
                            HashMap::new));
        }

    }

}

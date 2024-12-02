package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.model.DayTimeInterval;
import ch.sbb.pfi.netzgrafikeditor.converter.core.model.Identifiable;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

        sink.save(builder.build());
    }

    private void initialize(NetworkGraphic network) {
        lineCounter = new HashMap<>();
        lookup = new Lookup(network);
    }

    private void addStops() {
        log.info("Adding nodes of network graphic");
        for (Node node : lookup.nodes.values()) {
            log.debug("Adding node {}", node.getBetriebspunktName());
            builder.addStopFacility(node.getBetriebspunktName(), node.getPositionX(), node.getPositionY());
        }
    }

    private void addTrains() {
        log.info("Adding trains of network graphic");

        // setup trainrun section sequence builders; order and align sections
        HashMap<Integer, SectionSequenceBuilder> sequences = new HashMap<>();
        for (TrainrunSection section : lookup.sections.values()) {
            int trainId = section.getTrainrunId();
            SectionSequenceBuilder sequence = sequences.computeIfAbsent(trainId,
                    k -> new SectionSequenceBuilder(lookup.nodes));
            sequence.add(section);
        }

        // add a transit line with a transit route per direction for each train
        for (Map.Entry<Integer, SectionSequenceBuilder> entry : sequences.entrySet()) {
            Trainrun train = lookup.trains.get(entry.getKey());
            SectionSequenceBuilder sequence = entry.getValue();

            log.debug("Adding train {}", train.getName());
            createAndAddTransitLine(train, sequence.build());
        }
    }

    private void createAndAddTransitLine(Trainrun train, EnumMap<RouteDirection, List<TrainrunSection>> sequence) {

        String category = lookup.categories.get(train.getCategoryId()).getShortName();
        String lineId = createTransitLineId(train, sequence.get(RouteDirection.FORWARD), category);
        builder.addTransitLine(lineId, category);

        // add transit route for each direction
        for (RouteDirection direction : RouteDirection.values()) {
            createAndAddTransitRoute(lineId, train, sequence.get(direction), direction);
        }

    }

    private void createAndAddTransitRoute(String lineId, Trainrun train, List<TrainrunSection> sections, RouteDirection direction) {

        // get ordered trainrun nodes
        List<Node> nodes = new ArrayList<>();
        nodes.addFirst(lookup.nodes.get(sections.getFirst().getSourceNodeId()));
        sections.forEach(section -> nodes.add(lookup.nodes.get(section.getTargetNodeId())));

        // create transit route and add first route stop
        String routeId = createTransitRouteId(lineId, direction);
        Iterator<Node> nodeIter = nodes.iterator();
        Node sourceNode = nodeIter.next();
        String fachCategory = lookup.categories.get(train.getCategoryId()).getFachCategory();
        Duration dwellTimeAtOrigin = DwellTime.fromCategory(sourceNode, fachCategory);
        builder.addTransitRoute(routeId, lineId, sourceNode.getBetriebspunktName(), dwellTimeAtOrigin);

        // iterate over nodes and sections of transit route
        Iterator<TrainrunSection> sectionIter = sections.iterator();
        TrainrunSection nextSection = sectionIter.next();
        Duration travelTime = Duration.ofSeconds(0);
        for (int i = 1; i < nodes.size() - 1; i++) {

            Node targetNode = nodeIter.next();
            TrainrunSection currentSection = nextSection;
            nextSection = sectionIter.next();
            travelTime = travelTime.plusMinutes(Math.round(currentSection.getTravelTime().getTime()));

            // check if nonstop pass or stop at node
            if (isPass(targetNode, currentSection.getId())) {
                // pass: Add route pass to transit line
                builder.addRoutePass(routeId, targetNode.getBetriebspunktName());

            } else {
                // stop: Add route stop with dwell time from network graphic
                Duration dwellTime = DwellTime.fromSections(currentSection, nextSection);

                // check consistency of dwell time from network graphic against dwell time from category
                DwellTime.warnOnInconsistency(dwellTime, train, targetNode, fachCategory);

                // add stop and reset travel time
                builder.addRouteStop(routeId, targetNode.getBetriebspunktName(), travelTime, dwellTime);
                travelTime = Duration.ofSeconds(0);
            }
        }

        // add last stop
        Node targetNode = nodeIter.next();
        Duration dwellTimeAtDestination = DwellTime.fromCategory(targetNode, fachCategory);
        travelTime = travelTime.plusMinutes(Math.round(nextSection.getTravelTime().getTime()));
        builder.addRouteStop(routeId, targetNode.getBetriebspunktName(), travelTime, dwellTimeAtDestination);

        // prepare daytime intervals
        List<DayTimeInterval> timeIntervals = lookup.times.get(train.getTrainrunTimeCategoryId()).getDayTimeIntervals();
        if (timeIntervals.isEmpty()) {
            // add interval for full day in minutes if no interval is set
            timeIntervals.add(DayTimeInterval.builder()
                    .from((int) (Math.round(config.getServiceDayStart().toSecondOfDay() / SECONDS_PER_MINUTE)))
                    .to(((int) Math.round(config.getServiceDayEnd().toSecondOfDay() / SECONDS_PER_MINUTE)))
                    .build());
        }

        // derive departures in time intervals and add to supply builder
        List<ServiceDayTime> departures = createDepartureTimes(timeIntervals, train, sections.getFirst());
        log.debug("Adding departures to {} at: {}", routeId, departures);
        departures.forEach(departure -> builder.addDeparture(routeId, departure));
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
        final double hourOffset = firstSection.getSourceDeparture().getTime() * SECONDS_PER_MINUTE;
        final double frequency = lookup.frequencies.get(train.getFrequencyId()).getFrequency() * SECONDS_PER_MINUTE;
        final double frequencyOffset = lookup.frequencies.get(train.getFrequencyId()).getOffset() * SECONDS_PER_MINUTE;

        List<ServiceDayTime> departures = new ArrayList<>();
        for (DayTimeInterval dti : timeIntervals) {
            double fromTime = dti.getFrom() * SECONDS_PER_MINUTE;
            double toTime = dti.getTo() * SECONDS_PER_MINUTE;
            double departureTime = fromTime + frequencyOffset + hourOffset;
            while (departureTime < toTime) {
                departures.add(new ServiceDayTime((int) Math.round(departureTime)));
                departureTime += frequency;
            }
        }

        return departures;
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
                    .collect(Collectors.toMap(Identifiable::getId, element -> element, (element, element2) -> element,
                            HashMap::new));
        }

    }

}

package ch.sbb.pfi.netzgrafikeditor.converter;

import ch.sbb.pfi.netzgrafikeditor.converter.model.DayTimeInterval;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Identifiable;
import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Port;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Trainrun;
import ch.sbb.pfi.netzgrafikeditor.converter.model.TrainrunCategory;
import ch.sbb.pfi.netzgrafikeditor.converter.model.TrainrunCategoryHaltezeit;
import ch.sbb.pfi.netzgrafikeditor.converter.model.TrainrunFrequency;
import ch.sbb.pfi.netzgrafikeditor.converter.model.TrainrunSection;
import ch.sbb.pfi.netzgrafikeditor.converter.model.TrainrunTimeCategory;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Transition;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.RouteDirection;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.validation.NetworkGraphicValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class NetworkGraphicConverter {

    private final NetworkGraphicConverterConfig config;
    private final NetworkGraphicSource source;
    private final SupplyBuilder builder;
    private final ConverterSink sink;

    private Map<String, Integer> lineCounter;
    private Lookup lookup;

    /**
     * Get the dwell time of a train category at a node
     * <p>
     * If the fach category is uncategorized, then the connection time of the node is returned.
     *
     * @param node         the stop node.
     * @param fachCategory the fach category (A, B, C, D, IPV or Uncategorized).
     * @return the waiting time in seconds.
     */
    private static Duration getDwellTimeFromCategory(Node node, String fachCategory) {

        if (fachCategory.equals("HaltezeitUncategorized")) {
            Duration dwellTime = Duration.ofSeconds(node.getConnectionTime() * 60L);
            log.warn("Uncategorized dwell time category, returning connection time instead ({} s)",
                    dwellTime.toSeconds());
            return dwellTime;
        }

        TrainrunCategoryHaltezeit trainrunCategoryHaltezeit = node.getTrainrunCategoryHaltezeiten().get(fachCategory);
        if (trainrunCategoryHaltezeit == null) {
            String message = String.format("Invalid fachCategory value %s at node %s.", fachCategory,
                    node.getBetriebspunktName());
            throw new IllegalStateException(message);
        }

        if (trainrunCategoryHaltezeit.isNoHalt()) {
            return Duration.ofSeconds(0);
        }

        return Duration.ofSeconds(Math.round(trainrunCategoryHaltezeit.getHaltezeit() * 60));
    }

    private static Duration getDwellTimeFromSections(TrainrunSection currentSection, TrainrunSection nextSection) {
        double arrivalTime = currentSection.getTargetArrival().getTime();
        double departureTime = nextSection.getSourceDeparture().getTime();

        if (arrivalTime > departureTime) {
            // special case: The departure time is in the next hour
            // example: arrivalTime = 59, departureTime = 1; actual dwell time = 2 minutes
            return Duration.ofSeconds(Math.round((departureTime + 60 - arrivalTime) * 60.));
        } else {
            // normal case: Arrival time is before departure time within the same hour
            return Duration.ofSeconds(Math.round((departureTime - arrivalTime) * 60.));
        }
    }

    private void warnOnDwellTimeInconsistency(Duration dwellTime, Trainrun train, Node targetNode, String fachCategory, String lineId) {
        Duration dwellTimeFromCategory = getDwellTimeFromCategory(targetNode, fachCategory);
        if (!dwellTime.equals(dwellTimeFromCategory)) {
            log.warn(
                    "Trainrun {} (lineId: {}) has mismatch in dwell time at Stop {} for category {}: expected {}s, but found {}s.",
                    lookup.trains.get(train.getId()).getName(), lineId, targetNode.getBetriebspunktName(), fachCategory,
                    dwellTimeFromCategory.toSeconds(), dwellTime.toSeconds());
        }
    }

    public void run() throws IOException {
        log.info("Converting netzgrafik using source {}, supply builder {} and sink {}",
                source.getClass().getSimpleName(), builder.getClass().getSimpleName(), sink.getClass().getSimpleName());

        // load and validate network graphic
        NetworkGraphic networkGraphic = new NetworkGraphicValidator(config.getValidationStrategy(),
                config.isUseTrainNamesAsIds(), source.load()).run();

        initialize(networkGraphic);
        addStops();
        addTrains();

        sink.save();
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
        log.info("Adding trains");

        // setup trainrun section sequence builders
        HashMap<Integer, SectionSequenceBuilder> sequences = new HashMap<>();
        for (TrainrunSection section : lookup.sections.values()) {
            int trainId = section.getTrainrunId();
            SectionSequenceBuilder sequence = sequences.computeIfAbsent(trainId,
                    k -> new SectionSequenceBuilder(lookup.nodes));
            sequence.add(section);
        }

        // add a transit line with transit routes for each train
        for (Map.Entry<Integer, SectionSequenceBuilder> entry : sequences.entrySet()) {
            Trainrun train = lookup.trains.get(entry.getKey());
            SectionSequenceBuilder sequence = entry.getValue();

            log.debug("Adding train {}", train.getName());
            createAndAddTransitLine(train, sequence.build());
        }

        // build transit schedule
        builder.build();
    }

    private void createAndAddTransitLine(Trainrun train, List<TrainrunSection> sections) {

        // order trainrun nodes
        List<Node> nodes = new ArrayList<>();
        nodes.addFirst(lookup.nodes.get(sections.getFirst().getSourceNodeId()));
        sections.forEach(section -> nodes.add(lookup.nodes.get(section.getTargetNodeId())));

        // get vehicle type info from train category and create line id
        String vehicleType = lookup.categories.get(train.getCategoryId()).getShortName();
        String lineId = createTransitLineId(train, nodes, vehicleType);

        // add first route stop to transit line
        Iterator<Node> nodeIter = nodes.iterator();
        Node sourceNode = nodeIter.next();
        String fachCategory = lookup.categories.get(train.getCategoryId()).getFachCategory();
        Duration dwellTimeAtOrigin = getDwellTimeFromCategory(sourceNode, fachCategory);
        builder.addTransitLine(lineId, vehicleType, sourceNode.getBetriebspunktName(), dwellTimeAtOrigin);

        // iterate over nodes and sections of transit line
        Iterator<TrainrunSection> sectionIter = sections.iterator();
        TrainrunSection nextSection = sectionIter.next();
        Duration travelTime = Duration.ofSeconds(0);
        for (int i = 1; i < nodes.size() - 1; i++) {

            Node targetNode = nodeIter.next();
            TrainrunSection currentSection = nextSection;
            nextSection = sectionIter.next();
            travelTime = travelTime.plusMinutes(Math.round(currentSection.getTravelTime().getTime()));

            // check if it is a not nonstop transit pass or stop
            if (isPass(targetNode, currentSection.getId())) {
                // pass: Add route pass to transit line
                builder.addRoutePass(lineId, targetNode.getBetriebspunktName());

            } else {
                // stop: Add route stop with dwell time from network graphic to transit line
                Duration dwellTime = getDwellTimeFromSections(currentSection, nextSection);

                // check consistency of dwell time from network graphic against dwell time from category
                warnOnDwellTimeInconsistency(dwellTime, train, targetNode, fachCategory, lineId);

                // add stop and reset travel time
                builder.addRouteStop(lineId, targetNode.getBetriebspunktName(), travelTime, dwellTime);
                travelTime = Duration.ofSeconds(0);
            }
        }

        // add last stop
        Node targetNode = nodeIter.next();
        Duration dwellTimeAtDestination = getDwellTimeFromCategory(targetNode, fachCategory);
        travelTime = travelTime.plusMinutes(Math.round(nextSection.getTravelTime().getTime()));
        builder.addRouteStop(lineId, targetNode.getBetriebspunktName(), travelTime, dwellTimeAtDestination);

        // prepare daytime intervals
        List<DayTimeInterval> timeIntervals = lookup.times.get(train.getTrainrunTimeCategoryId()).getDayTimeIntervals();
        if (timeIntervals.isEmpty()) {
            // add interval for full day in minutes if no interval is set in the input
            timeIntervals.add(DayTimeInterval.builder()
                    .from((int) (Math.round(config.getServiceDayStart().toSecondOfDay() / 60.)))
                    .to(((int) Math.round(config.getServiceDayEnd().toSecondOfDay() / 60.)))
                    .build());
        }

        // create departures in intervals for both directions
        for (RouteDirection direction : RouteDirection.values()) {
            List<LocalTime> departures = createDepartureTimes(timeIntervals, train, sections, direction);
            log.debug("Add departures at: {}", departures);
            departures.forEach(departure -> builder.addDeparture(lineId, direction, departure));
        }

    }

    private String createTransitLineId(Trainrun train, List<Node> nodes, String vehicleType) {

        // check if option is set to use train name; also avoid name if it is empty (optional field in NGE)
        String lineId;
        if (config.isUseTrainNamesAsIds() && !train.getName().isBlank()) {
            lineId = train.getName();
        } else {
            // create id from vehicle type with origin and destination, ignore the train name from nge
            lineId = String.format("%s_%s_%s", vehicleType, nodes.getFirst().getBetriebspunktName(),
                    nodes.getLast().getBetriebspunktName());
        }

        // check if line id is already existing
        int count = lineCounter.getOrDefault(lineId, 0);
        if (count > 0) {
            lineCounter.put(lineId, ++count);
            log.info("Line with id {} is already existing, adding counter {} to id", lineId, count);
            lineId = String.format("%s_%d", lineId, count);
        } else {
            lineCounter.put(lineId, 1);
        }

        return lineId;
    }

    /**
     * Create departure times in day time intervals
     * <p>
     * For each defined time interval the departure times are calculated in seconds from midnight.
     *
     * @return a list with a time in seconds from midnight for each departure.
     */
    private List<LocalTime> createDepartureTimes(List<DayTimeInterval> timeIntervals, Trainrun train, List<TrainrunSection> sections, RouteDirection routeDirection) {
        final double hourOffset = switch (routeDirection) {
            case FORWARD -> sections.getFirst().getSourceDeparture().getTime() * 60.;
            case REVERSE -> sections.getLast().getTargetDeparture().getTime() * 60.;
        };
        final double frequency = lookup.frequencies.get(train.getFrequencyId()).getFrequency() * 60.;
        final double frequencyOffset = lookup.frequencies.get(train.getFrequencyId()).getOffset() * 60.;

        List<LocalTime> departures = new ArrayList<>();
        for (DayTimeInterval dti : timeIntervals) {
            double fromTime = dti.getFrom() * 60;
            double toTime = dti.getTo() * 60;
            double departureTime = fromTime + frequencyOffset + hourOffset;
            while (departureTime < toTime) {
                departures.add(LocalTime.ofSecondOfDay(Math.round(departureTime)));
                departureTime += frequency;
            }
        }

        return departures;
    }

    /**
     * Check if a node is a nonstop transit.
     * <p>
     * Orientation in mapping: Node --> Transition --> Port --> TrainrunSection --> Trainrun
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
     * Keep track of the NG elements, allows lookups by ids.
     */
    private static class Lookup {

        private final Map<Integer, Node> nodes;
        private final Map<Integer, Port> ports;
        private final Map<Integer, Transition> transitions;
        private final Map<Integer, Trainrun> trains;
        private final Map<Integer, TrainrunSection> sections;
        private final Map<Integer, TrainrunCategory> categories;
        private final Map<Integer, TrainrunFrequency> frequencies;
        private final Map<Integer, TrainrunTimeCategory> times;

        Lookup(NetworkGraphic network) {
            this.nodes = listToHashMap(network.getNodes());
            this.ports = listToHashMap(
                    network.getNodes().stream().map(Node::getPorts).flatMap(List::stream).collect(Collectors.toList()));
            this.transitions = listToHashMap(network.getNodes()
                    .stream()
                    .map(Node::getTransitions)
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
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

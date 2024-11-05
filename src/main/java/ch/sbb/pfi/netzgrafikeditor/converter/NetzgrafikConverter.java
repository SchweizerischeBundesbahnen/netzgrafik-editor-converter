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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class NetzgrafikConverter {

    private static final double OPERATION_DAY_START_TIME_SECONDS = 2 * 3600.;
    private static final double OPERATION_DAY_END_TIME_SECONDS = 23 * 3600.;

    private final NetworkGraphicSource source;
    private final SupplyBuilder supplyBuilder;
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
            log.warn("Uncategorized waiting time category, returning connection time instead ({})", dwellTime);
            return dwellTime;
        }

        TrainrunCategoryHaltezeit trainrunCategoryHaltezeit = node.getTrainrunCategoryHaltezeiten().get(fachCategory);
        if (trainrunCategoryHaltezeit == null) {
            String message = String.format("Invalid fachCategory value %s at node %s.", fachCategory,
                    node.getBetriebspunktName());
            throw new RuntimeException(message);
        }

        if (trainrunCategoryHaltezeit.isNoHalt()) {
            return Duration.ofSeconds(0);
        }

        return Duration.ofSeconds(Math.round(trainrunCategoryHaltezeit.getHaltezeit() * 60));
    }

    void run() throws IOException {
        log.info("Converting netzgrafik using source {}, supply builder {} and sink {}",
                source.getClass().getSimpleName(), supplyBuilder.getClass().getSimpleName(),
                sink.getClass().getSimpleName());

        initialize(source.load());
        addStops();
        addTrains();

        sink.save();
    }

    private void initialize(NetworkGraphic network) {
        lineCounter = new HashMap<>();
        lookup = new Lookup(network);
    }

    /**
     * Adding stops nodes from netzgrafik.
     */
    private void addStops() {
        log.info("Adding nodes of network graphic");
        for (Node node : lookup.nodes.values()) {
            log.debug("Adding node {}", node.getBetriebspunktName());
            supplyBuilder.addStopFacility(node.getBetriebspunktName());
        }
    }

    /**
     * Convert trains to transit lines and add to the MATSim schedule
     */
    private void addTrains() {
        log.info("Adding trains");

        // setup trainrun builder
        HashMap<Integer, TrainrunBuilder> trainToBuilder = new HashMap<>();
        for (TrainrunSection section : lookup.sections.values()) {
            int trainId = section.getTrainrunId();
            TrainrunBuilder trainrunBuilder = trainToBuilder.getOrDefault(trainId,
                    new TrainrunBuilder(lookup.trains.get(section.getTrainrunId())));
            trainrunBuilder.add(section);
            trainToBuilder.put(section.getTrainrunId(), trainrunBuilder);
        }

        // add each train (route & line)
        for (TrainrunBuilder tb : trainToBuilder.values()) {
            log.debug("Adding train {}", lookup.trains.get(tb.train.getId()).getName());
            // run builder to create ordered sections
            tb.build();
            // add transit routes and lines for both directions
            createAndAddTransitLine(tb.train, tb.nodes, tb.sections);
        }

        // build transit schedule
        supplyBuilder.build();
    }

    /**
     * Create and add a Trainrun to a TransitLine
     * <p>
     * Creates a TransitLine from the Trainrun ordered by the TrainrunBuilder. Adds a TransitLine with both directions
     * to the MATSim TransitSchedule. Further the Vehicles are assigned vehicle circuits (different route types)
     * depending on the departure times, the vehicle arrivals and vehicle turnaround time.
     *
     * @param train    trainrun object from the ng file.
     * @param nodes    the nodes in the trainrun.
     * @param sections the section in the trainrun.
     */
    private void createAndAddTransitLine(Trainrun train, List<Node> nodes, List<TrainrunSection> sections) {

        // get vehicle type info from train category and create line id
        String vehicleType = lookup.categories.get(train.getCategoryId()).getShortName();
        int count = lineCounter.getOrDefault(vehicleType, 0);
        int id = train.getId();
        String name = train.getName().trim();
        String lineId = String.format("%s_%s-%s_%s_%s", vehicleType, nodes.getFirst().getBetriebspunktName(),
                nodes.getLast().getBetriebspunktName(), id, name);
        lineCounter.put(vehicleType, ++count);

        // create route stops, rail links and travel times
        String fachCategory = lookup.categories.get(train.getCategoryId()).getFachCategory();
        Iterator<Node> nodeIter = nodes.iterator();
        Iterator<TrainrunSection> sectionIter = sections.iterator();
        Node sourceNode = nodeIter.next();
        Duration travelTime = Duration.ofSeconds(0);

        // add first route stop info
        Duration dwellTimeAtOrigin = getDwellTimeFromCategory(sourceNode, fachCategory);
        supplyBuilder.addTransitLine(lineId, vehicleType, sourceNode.getBetriebspunktName(), dwellTimeAtOrigin);

        // iterate over nodes and sections of transit line
        TrainrunSection nextSection = sectionIter.next();
        for (int i = 1; i < nodes.size() - 1; i++) {
            // add stop and check if it is a not nonstop transit stop
            Node targetNode = nodeIter.next();
            TrainrunSection currentSection = nextSection;
            nextSection = sectionIter.next();

            // pass: Add route stop info with zero waiting time
            travelTime = travelTime.plusMinutes(Math.round(currentSection.getTravelTime().getTime()));
            if (isPass(targetNode, currentSection.getId())) {
                supplyBuilder.addRoutePass(lineId, targetNode.getBetriebspunktName());
            } else {
                // stop: Add route stop info with waiting time from netzgrafik and reset travel time
                Duration dwellTimeFromCategory = getDwellTimeFromCategory(targetNode, fachCategory);
                Duration dwellTime = getDwellTimeFromSections(currentSection, nextSection);

                if (!dwellTime.equals(dwellTimeFromCategory)) {
                    log.warn(
                            "Trainrun {} has mismatch in dwell time at Stop {} for category {}: expected {}, but found {}.",
                            lookup.trains.get(train.getId()).getName(), targetNode.getBetriebspunktName(), fachCategory,
                            dwellTimeFromCategory, dwellTime);
                }

                supplyBuilder.addRouteStop(lineId, targetNode.getBetriebspunktName(), travelTime, dwellTime);
                travelTime = Duration.ofSeconds(0);
            }
        }

        // add last stop
        Node targetNode = nodeIter.next();
        Duration dwellTimeAtDestination = getDwellTimeFromCategory(targetNode, fachCategory);
        travelTime = travelTime.plusMinutes(Math.round(nextSection.getTravelTime().getTime()));
        supplyBuilder.addRouteStop(lineId, targetNode.getBetriebspunktName(), travelTime, dwellTimeAtDestination);

        // prepare daytime intervals
        List<DayTimeInterval> timeIntervals = lookup.times.get(train.getTrainrunTimeCategoryId()).getDayTimeIntervals();
        if (timeIntervals.isEmpty()) {
            // add interval for full day (in minutes)
            timeIntervals.add(DayTimeInterval.builder()
                    .from((int) (Math.round(OPERATION_DAY_START_TIME_SECONDS / 60.)))
                    .to(((int) Math.round(OPERATION_DAY_END_TIME_SECONDS / 60.)))
                    .build());
        }

        // create departures in intervals for both directions
        for (RouteDirection direction : RouteDirection.values()) {
            List<LocalTime> departures = createDepartureTimesInIntervals(timeIntervals, train, sections, direction);
            log.debug("Add departures at: {}", departures);
            departures.forEach(departure -> supplyBuilder.addDeparture(lineId, direction, departure));
        }

    }

    private Duration getDwellTimeFromSections(TrainrunSection currentSection, TrainrunSection nextSection) {
        double arrivalTime = currentSection.getTargetArrival().getTime();
        double departureTime = nextSection.getSourceDeparture().getTime();

        if (arrivalTime > departureTime) {
            // Special case: The departure time is in the next hour
            // Example: arrivalTime = 59, departureTime = 1; actual dwell time = 2 minutes
            return Duration.ofSeconds(Math.round((departureTime + 60 - arrivalTime) * 60.));
        } else {
            // Normal case: Arrival time is before departure time within the same hour
            return Duration.ofSeconds(Math.round((departureTime - arrivalTime) * 60.));
        }
    }

    /**
     * Create departure times in day time intervals
     * <p>
     * For each defined time interval the departure times are calculated in seconds from midnight.
     *
     * @return a list with a time in seconds from midnight for each departure.
     */
    private List<LocalTime> createDepartureTimesInIntervals(List<DayTimeInterval> timeIntervals, Trainrun train, List<TrainrunSection> sections, RouteDirection routeDirection) {
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

        for (var transition : nodeTransitions) {
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

    /**
     * Preprocess trainrun segments, since they are not ordered in NGE JSON export.
     */
    class TrainrunBuilder {

        private final Trainrun train;
        private final List<TrainrunSection> sections;
        private final List<Node> nodes;
        private final Map<Integer, List<TrainrunSection>> sources;
        private final Map<Integer, List<TrainrunSection>> targets;

        TrainrunBuilder(Trainrun train) {
            this.train = train;
            this.sections = new LinkedList<>(); // need to append to start of list
            this.nodes = new ArrayList<>();
            this.sources = new HashMap<>();
            this.targets = new HashMap<>();
        }

        private static TrainrunSection getNextTrainrunSection(int currentId, int id, Map<Integer, List<TrainrunSection>> trainrunSections) {
            TrainrunSection ts = null;
            if (trainrunSections.containsKey(id)) {
                ts = trainrunSections.get(id).stream().filter(t -> t.getId() != currentId).findFirst().orElse(null);
            }
            return ts;
        }

        void add(TrainrunSection section) {
            // add to sources
            List<TrainrunSection> sourcesList = sources.getOrDefault(section.getSourceNodeId(), new ArrayList<>());
            sourcesList.add(section);
            sources.put(section.getSourceNodeId(), sourcesList);

            // add to targets
            List<TrainrunSection> targetsList = targets.getOrDefault(section.getTargetNodeId(), new ArrayList<>());
            targetsList.add(section);
            targets.put(section.getTargetNodeId(), targetsList);

            log.debug("Adding section {} to trainrun builder for {} ({} --> {}, sources={} targets={})",
                    section.getId(), train.getName(),
                    lookup.nodes.get(section.getSourceNodeId()).getBetriebspunktName(),
                    lookup.nodes.get(section.getTargetNodeId()).getBetriebspunktName(), sourcesList.size(),
                    targetsList.size());
        }

        void build() {
            orderSections();
        }

        private void orderSections() {
            // add root section
            sections.addFirst(sources.entrySet().iterator().next().getValue().getFirst());

            // follow chain to terminal in target direction
            int count = 0;
            int maxIter = targets.size();
            boolean terminal = false;
            while (count < maxIter && !terminal) {
                terminal = searchTargetSection();
                count++;
            }

            // follow chain to terminal in target direction
            terminal = false;
            count = 0;
            while (count < maxIter && !terminal) {
                terminal = searchSourceSection();
                count++;
            }

            // add ordered nodes of trainrun
            nodes.addLast(lookup.nodes.get(sections.getFirst().getSourceNodeId()));
            sections.forEach(ts -> nodes.addLast(lookup.nodes.get(ts.getTargetNodeId())));
        }

        /**
         * Returns the next target section.
         * <p>
         * If the target section is not found inside the targets, then it is search inside the sources. If it is found,
         * it has to be swapped.
         */
        private boolean searchTargetSection() {
            TrainrunSection current = sections.getLast();
            int currentId = current.getId();
            int targetId = current.getTargetNodeId();

            TrainrunSection target = getNextTrainrunSection(currentId, targetId, sources);
            if (target != null) {
                logSectionAction(current, target, "target", "");
                sections.addLast(target);

                return false;
            }

            target = getNextTrainrunSection(currentId, targetId, targets);
            if (target != null) {
                logSectionAction(current, target, "target", ", swapped");
                target.swap();
                sections.addLast(target);

                return false;
            }
            log.debug("Builder arrived at target terminal");

            return true;
        }

        private boolean searchSourceSection() {
            TrainrunSection current = sections.getFirst();
            int currentId = current.getId();
            int sourceId = current.getSourceNodeId();

            TrainrunSection source = getNextTrainrunSection(currentId, sourceId, targets);
            if (source != null) {
                logSectionAction(current, source, "source", "");
                sections.addFirst(source);
                return false;
            }

            source = getNextTrainrunSection(currentId, sourceId, sources);
            if (source != null) {
                logSectionAction(current, source, "source", "");
                source.swap();
                sections.addFirst(source);
                return false;
            }

            log.debug("Builder arrived at source terminal");

            return true;
        }

        private void logSectionAction(TrainrunSection current, TrainrunSection target, String type, String description) {
            log.debug("Builder adding {} to section ({}: {} --> {}): ({}: {} --> {}){}", type, current.getId(),
                    lookup.nodes.get(current.getSourceNodeId()).getBetriebspunktName(),
                    lookup.nodes.get(current.getTargetNodeId()).getBetriebspunktName(), target.getId(),
                    lookup.nodes.get(target.getSourceNodeId()).getBetriebspunktName(),
                    lookup.nodes.get(target.getTargetNodeId()).getBetriebspunktName(), description);
        }
    }

}

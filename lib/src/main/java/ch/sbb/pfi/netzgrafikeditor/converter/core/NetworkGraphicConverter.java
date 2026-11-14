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
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopAccess;
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
import java.util.LinkedHashMap;
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
    private Map<ConnectionResolver.Endpoint, StopAccess> technicalStops;
    private Map<PatternKey, RoutePattern> patterns;
    private Set<String> servedStops;
    private List<ConnectionIssue> connectionIssues;

    private static String createTransitRouteId(String lineId, RouteDirection direction) {
        return String.format("%s_%s", lineId, direction.name());
    }

    /**
     * Converts minutes with decimals, as the network graphic gives them, into a duration rounded to the second.
     */
    private static Duration toDuration(double minutes) {
        return Duration.ofSeconds(Math.round(minutes * SECONDS_PER_MINUTE));
    }

    private static StopAccess combine(StopAccess first, StopAccess second) {
        return StopAccess.of(first.isBoarding() || second.isBoarding(), first.isAlighting() || second.isAlighting());
    }

    /**
     * Get the slack of a connection in seconds, from the arrival to the nearest departure of the other trainrun, given
     * the frequencies of both: a departure up to half their common period before the arrival is a missed connection
     * (negative slack), not a wait for almost a period.
     */
    private static int getSlack(RoutePattern arrivingPattern, Visit arrival, RoutePattern departingPattern, Visit departure) {
        int period = gcd(arrivingPattern.frequency(), departingPattern.frequency());
        int difference = departingPattern.departurePattern() + (int) departure.departure()
                .toSeconds() - arrivingPattern.departurePattern() - (int) arrival.arrival().toSeconds();

        return Math.floorMod(difference + period / 2, period) - period / 2;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /**
     * Get the consecutive departure time at the first node in seconds, which the departures repeat from.
     */
    private static int getDeparturePattern(TrainrunSection firstSection) {
        return (int) Math.round(firstSection.getSourceDeparture().getConsecutiveTime() * SECONDS_PER_MINUTE);
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
        handleConnectionIssues();

        sink.save(builder.build());
    }

    private void initialize(NetworkGraphic network) {
        if (config.isNonPassengerNetwork() && config.getNonPassengerCategories().isEmpty()) {
            throw new IllegalStateException("The non-passenger network needs non-passenger categories");
        }

        lineCounter = new HashMap<>();
        lookup = new Lookup(network);
        depots = config.getServiceDayBoundary() == ServiceDayBoundary.DEPOT ? findDepots(network) : Set.of();
        linesWithoutDepot = new TreeSet<>();
        technicalStops = Map.of();
        patterns = new HashMap<>();
        servedStops = new TreeSet<>();
        connectionIssues = new ArrayList<>();
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

        Map<Integer, EnumMap<RouteDirection, List<TrainrunSection>>> directedSequences = new LinkedHashMap<>();
        for (Map.Entry<Integer, SectionSequenceBuilder> entry : sequences.entrySet()) {
            Trainrun train = lookup.trains.get(entry.getKey());
            directedSequences.put(train.getId(), entry.getValue().build(train.getDirection()));
        }

        // resolve the connections before the routes: in the non-passenger network, they turn passes into stops
        ConnectionResolver.Resolution connections = new ConnectionResolver(lookup.nodes, lookup.ports, lookup.sections,
                lookup.trains, directedSequences).resolve();
        List<ConnectionResolver.Leg> legs = connections.legs();
        if (config.isNonPassengerNetwork()) {
            technicalStops = findTechnicalStops(legs);
        }

        // add a transit line with a transit route per direction for each train
        for (Map.Entry<Integer, EnumMap<RouteDirection, List<TrainrunSection>>> entry : directedSequences.entrySet()) {
            Trainrun train = lookup.trains.get(entry.getKey());
            if (config.isNonPassengerNetwork() && isPassenger(train)) {
                log.debug("Leaving out train {} with passenger service from the non-passenger network",
                        train.getName());
                continue;
            }

            log.debug("Adding train {}", train.getName());
            createAndAddTransitLine(train, entry.getValue());
        }

        addConnections(legs);
        reportUnresolvedConnections(connections.unresolved());

        // in the non-passenger network, a train changes to another trainrun only at a connection
        if (config.isNonPassengerNetwork()) {
            servedStops.forEach(builder::forbidTransfers);
        }
    }

    /**
     * Whether a trainrun carries passengers: unless its category is one without passenger service.
     */
    private boolean isPassenger(Trainrun train) {
        String category = lookup.categories.get(train.getCategoryId()).getShortName();
        return !config.getNonPassengerCategories().contains(category);
    }

    /**
     * Whether passengers can board and alight at the stops of a trainrun: in the non-passenger network, the trains
     * without passenger service are the ones routed; otherwise, nobody boards them.
     */
    private StopAccess stopAccess(Trainrun train) {
        return config.isNonPassengerNetwork() || isPassenger(train) ? StopAccess.BOARDING_AND_ALIGHTING :
                StopAccess.NONE;
    }

    /**
     * Find the passes that connections of the non-passenger network make a stop of: where a train alights from a
     * trainrun to change to another, or boards a trainrun after the change.
     */
    private Map<ConnectionResolver.Endpoint, StopAccess> findTechnicalStops(List<ConnectionResolver.Leg> legs) {
        Map<ConnectionResolver.Endpoint, StopAccess> stops = new HashMap<>();
        for (ConnectionResolver.Leg leg : legs) {
            if (isPassenger(lookup.trains.get(leg.arriving().trainrunId())) || isPassenger(
                    lookup.trains.get(leg.departing().trainrunId()))) {
                continue;
            }
            stops.merge(leg.arriving(), StopAccess.ALIGHTING_ONLY, NetworkGraphicConverter::combine);
            stops.merge(leg.departing(), StopAccess.BOARDING_ONLY, NetworkGraphicConverter::combine);
        }
        return stops;
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
        List<Visit> visits = createVisits(train, direction, sections, fachCategory);
        RoutePattern pattern = new RoutePattern(visits, getDeparturePattern(sections.getFirst()), getFrequency(train),
                new ArrayList<>());
        patterns.put(new PatternKey(train.getId(), direction), pattern);
        addTransitRoute(routeId, lineId, visits, 0, visits.size() - 1, fachCategory);

        // trainruns with day time intervals always run full trips, the boundary only applies to the whole service day
        List<DayTimeInterval> timeIntervals = lookup.times.get(train.getTrainrunTimeCategoryId()).getDayTimeIntervals();
        if (timeIntervals.isEmpty() && config.getServiceDayBoundary() != ServiceDayBoundary.FULL) {
            addBoundedDepartures(pattern, routeId, lineId, train, sections.getFirst(), fachCategory);
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
        departures.forEach(departure -> addDeparture(pattern, routeId, 0, visits.size() - 1, departure));
    }

    /**
     * Add a departure of a transit route that runs from the visit at index from to the visit at index to of the
     * pattern, and keep it to connect its trip.
     */
    private void addDeparture(RoutePattern pattern, String routeId, int from, int to, ServiceDayTime time) {
        builder.addDeparture(routeId, time);
        pattern.departures().add(new RouteDeparture(routeId, from, to, time));
    }

    /**
     * Create the visits of the nodes along the sections, with times relative to the departure at the first node.
     * <p>
     * The travel and dwell times of the network graphic have decimals (e.g. 1.5 minutes), so the elapsed time is summed
     * up unrounded and only each visit time is rounded, once, to the second. Rounding the durations instead would carry
     * the error of every section into all the visits after it.
     */
    private List<Visit> createVisits(Trainrun train, RouteDirection direction, List<TrainrunSection> sections, String fachCategory) {
        StopAccess access = stopAccess(train);
        List<Visit> visits = new ArrayList<>();
        Node sourceNode = lookup.nodes.get(sections.getFirst().getSourceNodeId());
        visits.add(new Visit(sourceNode, false, terminalDwellTime(sourceNode, fachCategory).negated(), Duration.ZERO,
                access));

        // minutes since the departure at the first node, never rounded
        double elapsed = 0;
        for (int i = 0; i < sections.size(); i++) {
            TrainrunSection currentSection = sections.get(i);
            Node targetNode = lookup.nodes.get(currentSection.getTargetNodeId());
            elapsed += currentSection.getTravelTime().getTime();
            Duration arrival = toDuration(elapsed);

            if (i == sections.size() - 1) {
                // last stop: dwell time from category
                Duration dwellTime = terminalDwellTime(targetNode, fachCategory);
                visits.add(new Visit(targetNode, false, arrival, arrival.plus(dwellTime), access));

            } else if (isPass(targetNode, currentSection.getId())) {
                StopAccess technicalStop = technicalStops.get(
                        new ConnectionResolver.Endpoint(train.getId(), direction, i + 1));
                if (technicalStop != null) {
                    // technical stop: the train changes to or from another trainrun here without stopping
                    visits.add(new Visit(targetNode, false, arrival, arrival, technicalStop));
                } else {
                    // pass: nonstop at node
                    visits.add(new Visit(targetNode, true, arrival, arrival, StopAccess.NONE));
                }

            } else {
                // stop: dwell time from network graphic
                double dwellTime = DwellTime.fromSections(currentSection, sections.get(i + 1));

                // check consistency of dwell time from network graphic against dwell time from category
                DwellTime.warnOnInconsistency(toDuration(dwellTime), train, targetNode, fachCategory);

                elapsed += dwellTime;
                visits.add(new Visit(targetNode, false, arrival, toDuration(elapsed), access));
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
        Duration dwellTimeAtOrigin = from == 0 ? origin.dwellTime() : terminalDwellTime(origin.node(), fachCategory);
        builder.addTransitRoute(routeId, lineId, origin.stopId(), dwellTimeAtOrigin, origin.access());
        servedStops.add(origin.stopId());

        Duration departure = origin.departure();
        for (int i = from + 1; i <= to; i++) {
            Visit visit = visits.get(i);
            if (visit.pass()) {
                builder.addRoutePass(routeId, visit.stopId());
                continue;
            }

            Duration dwellTime = i == to && to < visits.size() - 1 ? terminalDwellTime(visit.node(),
                    fachCategory) : visit.dwellTime();
            builder.addRouteStop(routeId, visit.stopId(), visit.arrival().minus(departure), dwellTime,
                    visit.access());
            servedStops.add(visit.stopId());
            departure = visit.departure();
        }
    }

    /**
     * Get the dwell time at the first or last node of a route: from the category, or none in the non-passenger network,
     * where a train changes to another trainrun in no time.
     */
    private Duration terminalDwellTime(Node node, String fachCategory) {
        return config.isNonPassengerNetwork() ? Duration.ZERO : DwellTime.fromCategory(node, fachCategory);
    }

    /**
     * Add the departures of the whole service day, with the trips at its start and end clipped to the service day.
     * <p>
     * A clipped trip starts at the first stop departing at or after the start and ends at the last stop arriving at or
     * before the end of the service day.
     */
    private void addBoundedDepartures(RoutePattern pattern, String routeId, String lineId, Trainrun train, TrainrunSection firstSection, String fachCategory) {
        final List<Visit> visits = pattern.visits();
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
            addDeparture(pattern, boundedRouteId, from, to, new ServiceDayTime(departureAtOrigin));
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

    /**
     * Add the connections of the trains in the output as connections between their trips: from each trip arriving at
     * the node to the trip departing from it as drawn.
     */
    private void addConnections(List<ConnectionResolver.Leg> legs) {
        for (ConnectionResolver.Leg leg : legs) {
            if (!isConverted(leg.connectionId(), leg.node(), lookup.trains.get(leg.arriving().trainrunId()),
                    lookup.trains.get(leg.departing().trainrunId()))) {
                continue;
            }

            RoutePattern arrivingPattern = patterns.get(
                    new PatternKey(leg.arriving().trainrunId(), leg.arriving().direction()));
            RoutePattern departingPattern = patterns.get(
                    new PatternKey(leg.departing().trainrunId(), leg.departing().direction()));
            Visit arrival = arrivingPattern.visits().get(leg.arriving().visitIndex());
            Visit departure = departingPattern.visits().get(leg.departing().visitIndex());
            if (arrival.pass() || departure.pass()) {
                reportConnectionIssue(ConnectionIssueType.AT_PASS, leg,
                        "a train of the connection passes the node without stopping");
                continue;
            }

            int slack = getSlack(arrivingPattern, arrival, departingPattern, departure);
            if (slack < 0) {
                reportConnectionIssue(ConnectionIssueType.MISSED, leg,
                        String.format("the departing train leaves %d s before the arriving train arrives", -slack));
                continue;
            }

            int connectionTime = (int) Math.round(leg.node().getConnectionTime() * SECONDS_PER_MINUTE);
            if (!config.isNonPassengerNetwork() && slack < connectionTime) {
                reportConnectionIssue(ConnectionIssueType.BELOW_CONNECTION_TIME, leg,
                        String.format("%d s to change, below the connection time of the node of %d s", slack,
                                connectionTime));
            }

            if (addTripConnections(leg, arrivingPattern, arrival, departingPattern, departure, slack) == 0) {
                reportConnectionIssue(ConnectionIssueType.WITHOUT_TRIPS, leg,
                        "no pair of trips within the service day");
            }
        }
    }

    /**
     * Add a connection from each trip arriving at the node to the trip departing from it the slack later, and return
     * how many; a trip without such a partner, e.g. of a trainrun running twice as often, has no connection.
     */
    private int addTripConnections(ConnectionResolver.Leg leg, RoutePattern arrivingPattern, Visit arrival, RoutePattern departingPattern, Visit departure, int slack) {
        int arrivalIndex = leg.arriving().visitIndex();
        int departureIndex = leg.departing().visitIndex();

        // the trips departing from the node by their time there: those whose route goes on from the node
        Map<Integer, RouteDeparture> departures = new HashMap<>();
        for (RouteDeparture candidate : departingPattern.departures()) {
            if (candidate.from() <= departureIndex && departureIndex < candidate.to()) {
                departures.put(candidate.timeAt(departingPattern, departure.departure()), candidate);
            }
        }

        // the trips arriving at the node: those whose route comes from before the node
        int count = 0;
        for (RouteDeparture candidate : arrivingPattern.departures()) {
            if (candidate.from() < arrivalIndex && arrivalIndex <= candidate.to()) {
                RouteDeparture connecting = departures.get(
                        candidate.timeAt(arrivingPattern, arrival.arrival()) + slack);
                if (connecting != null) {
                    builder.addConnection(leg.node().getBetriebspunktName(), candidate.routeId(), candidate.time(),
                            connecting.routeId(), connecting.time());
                    count++;
                }
            }
        }

        return count;
    }

    private void reportUnresolvedConnections(List<ConnectionResolver.Unresolved> connections) {
        for (ConnectionResolver.Unresolved connection : connections) {
            Trainrun train1 = lookup.trains.get(connection.trainrunId1());
            Trainrun train2 = lookup.trains.get(connection.trainrunId2());
            if (isConverted(connection.connectionId(), connection.node(), train1, train2)) {
                reportConnectionIssue(ConnectionIssueType.AGAINST_DIRECTION, connection.connectionId(),
                        connection.node(), train1, train2,
                        "neither trainrun arrives at the node where the other departs from it");
            }
        }
    }

    /**
     * Whether the connection between the trainruns is converted: in the non-passenger network between trainruns without
     * passenger service, otherwise between trainruns with passenger service. Reports a connection to a trainrun that
     * the non-passenger network leaves out.
     */
    private boolean isConverted(int connectionId, Node node, Trainrun from, Trainrun to) {
        boolean fromPassenger = isPassenger(from);
        boolean toPassenger = isPassenger(to);

        if (config.isNonPassengerNetwork()) {
            if (fromPassenger && toPassenger) {
                return false; // neither trainrun is in the non-passenger network
            }
            if (fromPassenger || toPassenger) {
                reportConnectionIssue(ConnectionIssueType.TO_EXCLUDED_TRAINRUN, connectionId, node, from, to,
                        "connects to a trainrun with passenger service, which the non-passenger network leaves out");
                return false;
            }
            return true;
        }

        if (!fromPassenger || !toPassenger) {
            log.debug("Leaving out connection {} at node {}, a trainrun has no passenger service", connectionId,
                    node.getBetriebspunktName());
            return false;
        }
        return true;
    }

    private void reportConnectionIssue(ConnectionIssueType type, ConnectionResolver.Leg leg, String detail) {
        reportConnectionIssue(type, leg.connectionId(), leg.node(), lookup.trains.get(leg.arriving().trainrunId()),
                lookup.trains.get(leg.departing().trainrunId()), detail);
    }

    private void reportConnectionIssue(ConnectionIssueType type, int connectionId, Node node, Trainrun from, Trainrun to, String detail) {
        connectionIssues.add(
                new ConnectionIssue(type, connectionId, node.getBetriebspunktName(), describe(from), describe(to),
                        detail));
    }

    /**
     * Handle the connections that cannot be converted as drawn, according to the validation strategy; they cannot be
     * repaired, so the repairing strategies warn about them.
     */
    private void handleConnectionIssues() {
        if (connectionIssues.isEmpty()) {
            return;
        }

        switch (config.getValidationStrategy()) {
            case SKIP_VALIDATION -> connectionIssues.forEach(issue -> log.debug("Invalid connection: {}", issue));
            case FAIL_ON_ISSUES -> {
                connectionIssues.forEach(issue -> log.warn("Invalid connection: {}", issue));
                throw new IllegalStateException(String.format(
                        "Found %d connections that cannot be converted as drawn and option fail on issues is set.",
                        connectionIssues.size()));
            }
            default -> connectionIssues.forEach(issue -> log.warn("Invalid connection: {}", issue));
        }
    }

    private String describe(Trainrun train) {
        String category = lookup.categories.get(train.getCategoryId()).getShortName();
        return String.format("%s %s", category, train.getName().isBlank() ? "#" + train.getId() : train.getName()
                .strip());
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
        return time + Math.floorMod(getDeparturePattern(firstSection) - time, getFrequency(train));
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

    private enum ConnectionIssueType {
        /**
         * The departing train leaves before the arriving train arrives.
         */
        MISSED,
        /**
         * Less time to change than the connection time of the node, the Netzgrafik-Editor's own warning; the connection
         * is converted, as the departing train waits.
         */
        BELOW_CONNECTION_TIME,
        /**
         * A train of the connection passes the node without stopping, so passengers cannot change there.
         */
        AT_PASS,
        /**
         * The non-passenger network leaves out the trainrun with passenger service of the connection.
         */
        TO_EXCLUDED_TRAINRUN,
        /**
         * No trip arriving has a trip departing the slack later within the service day.
         */
        WITHOUT_TRIPS,
        /**
         * Neither trainrun arrives at the node where the other departs from it, e.g. two one-way trainruns that both
         * end there.
         */
        AGAINST_DIRECTION
    }

    /**
     * A trainrun in a direction, as the key of its {@link RoutePattern}.
     */
    private record PatternKey(int trainrunId, RouteDirection direction) {
    }

    /**
     * The visits of a trainrun in a direction, the departure pattern and frequency they repeat with (in seconds), and
     * the departures of its routes: the full route, or the routes clipped to the service day.
     */
    private record RoutePattern(List<Visit> visits, int departurePattern, int frequency,
                                List<RouteDeparture> departures) {
    }

    /**
     * A departure of a route that runs from the visit at index from to the visit at index to of its pattern, by its
     * time at the visit at index from.
     */
    private record RouteDeparture(String routeId, int from, int to, ServiceDayTime time) {

        /**
         * Get the time of the trip at a visit of its pattern in seconds, from the offset of the visit relative to the
         * departure at the first node of the pattern.
         */
        private int timeAt(RoutePattern pattern, Duration offset) {
            return time.toSecondOfDay() - (int) pattern.visits()
                    .get(from)
                    .departure()
                    .toSeconds() + (int) offset.toSeconds();
        }

    }

    private record ConnectionIssue(ConnectionIssueType type, int connectionId, String node, String from, String to,
                                   String detail) {

        @Override
        public String toString() {
            return String.format("type=%s, connection=%d, node=%s, from=%s, to=%s, %s", type, connectionId, node, from,
                    to, detail);
        }

    }

    /**
     * Visit of a node on a transit route, with arrival and departure relative to the departure at the first node.
     */
    private record Visit(Node node, boolean pass, Duration arrival, Duration departure, StopAccess access) {

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

        /**
         * Get the dwell time at the node between two sections in minutes, unrounded.
         */
        private static double fromSections(TrainrunSection currentSection, TrainrunSection nextSection) {
            double arrivalTime = currentSection.getTargetArrival().getTime();
            double departureTime = nextSection.getSourceDeparture().getTime();

            if (arrivalTime > departureTime) {
                // special case: The departure time is in the next hour
                // example: arrivalTime = 59, departureTime = 1; actual dwell time = 2 minutes
                return departureTime + 60 - arrivalTime;
            } else {
                // normal case: Arrival time is before departure time within the same hour
                return departureTime - arrivalTime;
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

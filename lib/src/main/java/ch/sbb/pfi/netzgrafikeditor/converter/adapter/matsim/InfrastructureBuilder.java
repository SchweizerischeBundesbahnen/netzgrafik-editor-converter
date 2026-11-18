package ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteElement;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteElementVisitor;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RoutePass;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteStop;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackNodeInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackSegmentInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitRouteInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.Coordinate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the stop facilities and the network the transit routes run on.
 * <p>
 * The network has one node per node of the track infrastructure, identified by its id: the node of a stop, or a
 * junction named by the infrastructure repository. Segments that meet at a junction therefore share its node, whichever
 * route added them first, and a transit route follows one continuous path through the network. Tracks that do not form
 * such a path are reported according to the validation strategy, see {@link #handleTrackIssues()}.
 */
@Slf4j
@RequiredArgsConstructor
class InfrastructureBuilder {

    public static final double STOP_LINK_LENGTH = 0.01;

    private final MatsimSupplyFactory factory;
    private final InfrastructureRepository infrastructureRepository;
    private final ValidationStrategy validationStrategy;

    private final Map<String, TransitStopFacility> stopFacilities = new HashMap<>();
    private final Map<String, AddedNode> addedNodes = new HashMap<>();
    private final Map<String, AddedSegment> addedSegments = new HashMap<>();
    private final List<TrackIssue> trackIssues = new ArrayList<>();

    /**
     * Lists what a segment has other than the segment with its id that was added first: other nodes, another length or
     * other link attributes. The coordinates of the nodes are checked with the nodes themselves.
     */
    private static List<String> differences(TrackSegmentInfo added, TrackSegmentInfo segment) {
        List<String> differences = new ArrayList<>();

        if (!added.getFromNode().getId().equals(segment.getFromNode().getId()) || !added.getToNode()
                .getId()
                .equals(segment.getToNode().getId())) {
            differences.add(String.format("connects %s to %s instead of %s to %s", segment.getFromNode().getId(),
                    segment.getToNode().getId(), added.getFromNode().getId(), added.getToNode().getId()));
        }

        if (Double.compare(added.getLength(), segment.getLength()) != 0) {
            differences.add(String.format("length %s instead of %s", segment.getLength(), added.getLength()));
        }

        if (!added.getLinkAttributes().equals(segment.getLinkAttributes())) {
            differences.add(String.format("link attributes %s instead of %s", segment.getLinkAttributes(),
                    added.getLinkAttributes()));
        }

        return differences;
    }

    TransitStopFacility buildTransitStopFacility(StopFacilityInfo stopFacilityInfo) {
        String stopId = stopFacilityInfo.getId();

        Coord coord = new Coord(stopFacilityInfo.getCoordinate().getLongitude(),
                stopFacilityInfo.getCoordinate().getLatitude());
        Node node = factory.createNode(String.format("%s", stopId), coord);
        Link stopLink = factory.createLink(LinkType.STOP, node, node, STOP_LINK_LENGTH,
                stopFacilityInfo.getAttributes());

        TransitStopFacility stop = factory.createTransitStopFacility(stopId, stopLink);
        stop.setName(stopFacilityInfo.getName());
        stopFacilities.put(stopId, stop);

        // the node of the stop is the node of the track infrastructure with the id of the stop
        addedNodes.put(stopId, new AddedNode(node, stopFacilityInfo.getCoordinate()));

        return stop;
    }

    TransitRoute buildTransitRoute(TransitRouteInfo transitRouteInfo, List<RouteElement> routeElements) {
        TransitLine transitLine = factory.getOrCreateTransitLine(transitRouteInfo.getTransitLineInfo().getId());
        List<Id<Link>> routeLinks = new ArrayList<>();
        List<TransitRouteStop> routeStops = new ArrayList<>();
        final double[] travelTime = {0};

        // add first stop
        RouteStop firstRouteStop = (RouteStop) routeElements.getFirst();
        final TransitStopFacility[] stopFacility = {stopFacilities.get(firstRouteStop.getStopFacilityInfo().getId())};
        TransitRouteStop transitRouteStop = factory.createTransitRouteStop(stopFacility[0], OptionalTime.undefined(),
                OptionalTime.zeroSeconds(), firstRouteStop.getAccess());
        routeStops.add(transitRouteStop);
        routeLinks.add(stopFacility[0].getLinkId());

        // loop over route elements, set first stop as last element and start with second element
        for (int i = 1; i < routeElements.size(); i++) {
            boolean lastStop = i == routeElements.size() - 1;
            RouteElement lastElement = routeElements.get(i - 1);
            RouteElement currentElement = routeElements.get(i);

            // visit element
            currentElement.accept(new RouteElementVisitor() {

                @Override
                public void visit(RouteStop routeStop) {
                    travelTime[0] = travelTime[0] + routeStop.getTravelTime().toSeconds();
                    double dwellTime = routeStop.getDwellTime().toSeconds();

                    // define offset times
                    OptionalTime arrivalOffset = OptionalTime.defined(travelTime[0]);
                    OptionalTime departureOffset = lastStop ? OptionalTime.undefined() : OptionalTime.defined(
                            travelTime[0] + dwellTime);

                    // add route stop
                    stopFacility[0] = stopFacilities.get(routeStop.getStopFacilityInfo().getId());
                    TransitRouteStop transitRouteStop = factory.createTransitRouteStop(stopFacility[0], arrivalOffset,
                            departureOffset, routeStop.getAccess());
                    routeStops.add(transitRouteStop);

                    travelTime[0] = travelTime[0] + dwellTime;
                }

                @Override
                public void visit(RoutePass routePass) {
                    stopFacility[0] = stopFacilities.get(routePass.getStopFacilityInfo().getId());
                }

            });

            // connect stop facilities on network and add stop link
            routeLinks.addAll(connect(transitRouteInfo, lastElement, currentElement));
            routeLinks.add(stopFacility[0].getLinkId());
        }

        return factory.createTransitRoute(transitLine, transitRouteInfo.getId(),
                transitRouteInfo.getTransitLineInfo().getTransportMode().name().toLowerCase(), routeLinks, routeStops);
    }

    /**
     * Reports the tracks of the infrastructure repository that do not form a continuous path between their stops: as
     * debug messages when skipping validation, failing the conversion when failing on issues, and as warnings
     * otherwise. A track cannot be repaired, so the repairing strategies warn as well; the network is built as the
     * repository gave it.
     */
    void handleTrackIssues() {
        if (trackIssues.isEmpty()) {
            return;
        }

        switch (validationStrategy) {
            case SKIP_VALIDATION -> trackIssues.forEach(issue -> log.debug("Invalid track: {}", issue));
            case FAIL_ON_ISSUES -> {
                trackIssues.forEach(issue -> log.warn("Invalid track: {}", issue));
                throw new IllegalStateException(String.format(
                        "Found %d invalid tracks in the infrastructure repository and option fail on issues is set.",
                        trackIssues.size()));
            }
            default -> trackIssues.forEach(issue -> log.warn("Invalid track: {}", issue));
        }
    }

    // connects transit route stops on network, calls infrastructure repository for track information
    private List<Id<Link>> connect(TransitRouteInfo transitRouteInfo, RouteElement from, RouteElement to) {
        StopFacilityInfo fromStop = from.getStopFacilityInfo();
        StopFacilityInfo toStop = to.getStopFacilityInfo();
        List<TrackSegmentInfo> segments = infrastructureRepository.getTrack(fromStop, toStop, transitRouteInfo);
        checkTrack(transitRouteInfo, fromStop, toStop, segments);

        List<Id<Link>> linkIds = new ArrayList<>();
        for (TrackSegmentInfo segment : segments) {
            linkIds.add(getOrCreateSegmentLink(transitRouteInfo, fromStop, toStop, segment));
        }

        return linkIds;
    }

    private void checkTrack(TransitRouteInfo transitRouteInfo, StopFacilityInfo fromStop, StopFacilityInfo toStop, List<TrackSegmentInfo> segments) {
        if (segments.isEmpty()) {
            reportIssue(TrackIssueType.NOT_AT_STOPS, transitRouteInfo, fromStop, toStop, "track has no segments");
            return;
        }

        String firstNodeId = segments.getFirst().getFromNode().getId();
        if (!firstNodeId.equals(fromStop.getId())) {
            reportIssue(TrackIssueType.NOT_AT_STOPS, transitRouteInfo, fromStop, toStop,
                    String.format("track starts at node %s", firstNodeId));
        }

        String lastNodeId = segments.getLast().getToNode().getId();
        if (!lastNodeId.equals(toStop.getId())) {
            reportIssue(TrackIssueType.NOT_AT_STOPS, transitRouteInfo, fromStop, toStop,
                    String.format("track ends at node %s", lastNodeId));
        }

        for (int i = 1; i < segments.size(); i++) {
            TrackSegmentInfo previous = segments.get(i - 1);
            TrackSegmentInfo current = segments.get(i);
            if (!current.getFromNode().getId().equals(previous.getToNode().getId())) {
                reportIssue(TrackIssueType.NOT_CONTIGUOUS, transitRouteInfo, fromStop, toStop,
                        String.format("segment %s ends at node %s, segment %s starts at node %s",
                                previous.getSegmentId(), previous.getToNode().getId(), current.getSegmentId(),
                                current.getFromNode().getId()));
            }
        }
    }

    private Id<Link> getOrCreateSegmentLink(TransitRouteInfo transitRouteInfo, StopFacilityInfo fromStop, StopFacilityInfo toStop, TrackSegmentInfo segment) {
        // a segment places its nodes where they were first added, whether the segment itself is new or not
        checkNodeCoordinate(transitRouteInfo, fromStop, toStop, segment.getFromNode());
        checkNodeCoordinate(transitRouteInfo, fromStop, toStop, segment.getToNode());

        AddedSegment added = addedSegments.get(segment.getSegmentId());

        // a segment already added keeps its link, with the nodes, length and attributes it was first added with
        if (added != null) {
            log.debug("Track segment {} already added, reusing its link", segment.getSegmentId());
            List<String> differences = differences(added.segment(), segment);
            if (!differences.isEmpty()) {
                reportIssue(TrackIssueType.SEGMENT_CONFLICT, transitRouteInfo, fromStop, toStop,
                        String.format("segment %s differs from when it was added: %s", segment.getSegmentId(),
                                String.join(", ", differences)));
            }
            return added.linkId();
        }

        log.debug("Adding track segment {}", segment.getSegmentId());
        Node fromNode = getOrCreateNode(segment.getFromNode());
        Node toNode = getOrCreateNode(segment.getToNode());
        Id<Link> linkId = factory.createLink(LinkType.ROUTE, segment.getSegmentId(), fromNode, toNode,
                segment.getLength(), segment.getLinkAttributes()).getId();
        addedSegments.put(segment.getSegmentId(), new AddedSegment(linkId, segment));

        return linkId;
    }

    private void checkNodeCoordinate(TransitRouteInfo transitRouteInfo, StopFacilityInfo fromStop, StopFacilityInfo toStop, TrackNodeInfo trackNode) {
        AddedNode added = addedNodes.get(trackNode.getId());
        if (added != null && !added.coordinate().equals(trackNode.getCoordinate())) {
            reportIssue(TrackIssueType.NODE_CONFLICT, transitRouteInfo, fromStop, toStop,
                    String.format("node %s is at %s, but was added at %s", trackNode.getId(),
                            trackNode.getCoordinate(), added.coordinate()));
        }
    }

    /**
     * Gets the network node of a node of the track infrastructure, created at its coordinate if not added yet; a node
     * already added keeps the coordinate it was first added at (see {@link #checkNodeCoordinate}).
     */
    private Node getOrCreateNode(TrackNodeInfo trackNode) {
        AddedNode added = addedNodes.get(trackNode.getId());
        if (added != null) {
            return added.node();
        }

        Node node = factory.createNode(trackNode.getId(),
                new Coord(trackNode.getCoordinate().getLongitude(), trackNode.getCoordinate().getLatitude()));
        addedNodes.put(trackNode.getId(), new AddedNode(node, trackNode.getCoordinate()));

        return node;
    }

    private void reportIssue(TrackIssueType type, TransitRouteInfo transitRouteInfo, StopFacilityInfo fromStop, StopFacilityInfo toStop, String detail) {
        trackIssues.add(new TrackIssue(type, transitRouteInfo.getId(), fromStop.getId(), toStop.getId(), detail));
    }

    private enum TrackIssueType {
        /**
         * The track does not start at the node of the from stop or does not end at the node of the to stop.
         */
        NOT_AT_STOPS,
        /**
         * A segment does not start at the node the segment before ends at.
         */
        NOT_CONTIGUOUS,
        /**
         * A segment id stands for another segment than it did before: other nodes, another length or other link
         * attributes.
         */
        SEGMENT_CONFLICT,
        /**
         * A node id stands at another coordinate than it did before.
         */
        NODE_CONFLICT
    }

    private record TrackIssue(TrackIssueType type, String routeId, String fromStopId, String toStopId, String detail) {

        @Override
        public String toString() {
            return String.format("type=%s, route=%s, from=%s, to=%s, %s", type, routeId, fromStopId, toStopId, detail);
        }

    }

    private record AddedNode(Node node, Coordinate coordinate) {
    }

    private record AddedSegment(Id<Link> linkId, TrackSegmentInfo segment) {
    }

}

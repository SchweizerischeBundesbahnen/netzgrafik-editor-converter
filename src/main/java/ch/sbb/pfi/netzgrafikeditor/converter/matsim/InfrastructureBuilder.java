package ch.sbb.pfi.netzgrafikeditor.converter.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteDirection;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteElement;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteElementVisitor;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RoutePass;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteStop;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackSegmentInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitLineInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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

@Slf4j
@RequiredArgsConstructor
class InfrastructureBuilder {

    public static final double STOP_LINK_LENGTH = 0.01;

    private final Scenario scenario;
    private final MatsimSupplyFactory factory;
    private final InfrastructureRepository infrastructureRepository;

    private final Map<String, TransitStopFacility> stopFacilities = new HashMap<>();
    private final Map<String, Id<Link>> addedSegments = new HashMap<>();

    TransitStopFacility buildTransitStopFacility(StopFacilityInfo stopFacilityInfo) {
        String stopId = stopFacilityInfo.getId();

        Coord coord = new Coord(stopFacilityInfo.getCoordinate().getLongitude(),
                stopFacilityInfo.getCoordinate().getLatitude());
        Node node = factory.createNode(String.format("%s", stopId), coord);
        Link stopLink = factory.createLink(LinkType.STOP, node, node, STOP_LINK_LENGTH,
                stopFacilityInfo.getLinkAttributes());

        TransitStopFacility stop = factory.createTransitStopFacility(stopId, stopLink);
        stopFacilities.put(stopId, stop);

        return stop;
    }

    TransitRoute buildTransitRoute(TransitLineInfo transitLineInfo, List<RouteElement> routeElements, RouteDirection direction) {
        TransitLine transitLine = factory.getOrCreateTransitLine(transitLineInfo.getId());
        List<Id<Link>> routeLinks = new ArrayList<>();
        List<TransitRouteStop> routeStops = new ArrayList<>();
        final double[] travelTime = {0};

        // correct for route direction
        List<RouteElement> directedRouteElements = switch (direction) {
            case FORWARD -> routeElements;
            case REVERSE -> routeElements.reversed();
        };

        // add first stop
        RouteStop firstRouteStop = (RouteStop) directedRouteElements.getFirst();
        final TransitStopFacility[] stopFacility = {stopFacilities.get(firstRouteStop.getStopFacilityInfo().getId())};
        TransitRouteStop transitRouteStop = factory.createTransitRouteStop(stopFacility[0], OptionalTime.undefined(),
                OptionalTime.zeroSeconds());
        routeStops.add(transitRouteStop);
        routeLinks.add(stopFacility[0].getLinkId());

        // loop over route elements, set first stop as last element and start with second element
        for (int i = 1; i < directedRouteElements.size(); i++) {
            boolean lastStop = i == directedRouteElements.size() - 1;
            RouteElement lastElement = directedRouteElements.get(i - 1);
            RouteElement currentElement = directedRouteElements.get(i);

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
                            departureOffset);
                    routeStops.add(transitRouteStop);

                    travelTime[0] = travelTime[0] + dwellTime;
                }

                @Override
                public void visit(RoutePass routePass) {
                    stopFacility[0] = stopFacilities.get(routePass.getStopFacilityInfo().getId());
                }

            });

            // connect stop facilities on network and add stop link
            routeLinks.addAll(connect(stopFacilities, addedSegments, transitLineInfo, lastElement, currentElement));
            routeLinks.add(stopFacility[0].getLinkId());
        }

        return factory.createTransitRoute(transitLine,
                String.format("%s_%s", transitLineInfo.getId(), direction.name()), routeLinks, routeStops);
    }

    // connects transit route stops on network, calls infrastructure repository for track information
    private List<Id<Link>> connect(Map<String, TransitStopFacility> stopFacilities, Map<String, Id<Link>> addedSegments, TransitLineInfo transitLineInfo, RouteElement from, RouteElement to) {
        List<TrackSegmentInfo> segments = infrastructureRepository.getTrack(from.getStopFacilityInfo(),
                to.getStopFacilityInfo(), transitLineInfo);

        List<Id<Link>> linkIds = new ArrayList<>();
        int count = 0;
        for (TrackSegmentInfo segment : segments) {

            // check if segment was already added
            if (addedSegments.containsKey(segment.getSegmentId())) {

                log.debug("Track segment {} already added, skipping", segment.getSegmentId());
                linkIds.add(addedSegments.get(segment.getSegmentId()));

            } else { // segment is new, create link with nodes

                log.debug("Adding track segment {}", segment.getSegmentId());
                Node fromNode;
                Node toNode;

                // if first segment
                if (count == 0) {
                    TransitStopFacility fromStop = stopFacilities.get(from.getStopFacilityInfo().getId());
                    Link fromLink = scenario.getNetwork().getLinks().get(fromStop.getLinkId());
                    fromNode = fromLink.getToNode();
                } else {
                    fromNode = factory.createNode(String.format("%s_from_node", segment.getSegmentId()),
                            new Coord(segment.getFromCoordinate().getLongitude(),
                                    segment.getFromCoordinate().getLatitude()));
                }

                // if last segment
                if (count == segments.size() - 1) {
                    TransitStopFacility toStop = stopFacilities.get(to.getStopFacilityInfo().getId());
                    Link toLink = scenario.getNetwork().getLinks().get(toStop.getLinkId());
                    toNode = toLink.getFromNode();
                } else {
                    toNode = factory.createNode(String.format("%s_to_node", segment.getSegmentId()),
                            new Coord(segment.getToCoordinate().getLongitude(),
                                    segment.getToCoordinate().getLatitude()));
                }

                Id<Link> segmentLink = factory.createLink(LinkType.ROUTE, fromNode, toNode, segment.getLength(),
                        segment.getLinkAttributes()).getId();
                addedSegments.put(segment.getSegmentId(), segmentLink);
                linkIds.add(segmentLink);

            }

            count++;
        }

        return linkIds;
    }

}

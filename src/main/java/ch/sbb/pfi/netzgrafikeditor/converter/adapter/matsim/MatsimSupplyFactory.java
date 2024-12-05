package ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim;

import lombok.extern.slf4j.Slf4j;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Slf4j
class MatsimSupplyFactory {

    private final TransitSchedule schedule;
    private final Vehicles vehicles;
    private final Network network;
    private final TransitScheduleFactory sf;
    private final VehiclesFactory vf;
    private final NetworkFactory nf;

    MatsimSupplyFactory(Scenario scenario) {
        schedule = scenario.getTransitSchedule();
        vehicles = scenario.getTransitVehicles();
        network = scenario.getNetwork();
        sf = schedule.getFactory();
        vf = vehicles.getFactory();
        nf = network.getFactory();
    }

    private static void putAttributes(Attributable object, Map<String, Object> attributes) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            object.getAttributes().putAttribute(entry.getKey(), entry.getValue());
        }
    }

    Departure createDeparture(String id, double time) {
        Id<Departure> departureId = Id.create(String.format(IdPattern.DEPARTURE, id), Departure.class);
        log.debug("Creating Departure {}", departureId);

        return sf.createDeparture(departureId, time);
    }

    TransitRoute createTransitRoute(TransitLine transitLine, String id, List<Id<Link>> routeLinks, List<TransitRouteStop> stops) {
        Id<TransitRoute> routeId = Id.create(String.format(IdPattern.TRANSIT_ROUTE, id), TransitRoute.class);
        NetworkRoute networkRoute = RouteUtils.createNetworkRoute(routeLinks);
        TransitRoute transitRoute = transitLine.getRoutes().get(routeId);

        if (transitRoute != null) {
            log.warn("TransitRoute {} is already existing on TransitLine {}", routeId, transitLine.getId());
            return transitRoute;
        }

        log.debug("Creating TransitRoute {} and adding to TransitLine {}", routeId, transitLine.getId());
        transitRoute = sf.createTransitRoute(routeId, networkRoute, stops, Default.LINK_MODE);
        transitLine.addRoute(transitRoute);

        return transitRoute;
    }

    TransitRouteStop createTransitRouteStop(TransitStopFacility transitStopFacility, OptionalTime arrivalOffset, OptionalTime departureOffset) {
        log.debug("Creating TransitRouteStop at TransitStopFacility {}", transitStopFacility.getId());
        TransitRouteStop transitRouteStop = sf.createTransitRouteStop(transitStopFacility, arrivalOffset,
                departureOffset);
        transitRouteStop.setAwaitDepartureTime(true);

        return transitRouteStop;
    }

    TransitStopFacility createTransitStopFacility(String id, Link link) {
        Id<TransitStopFacility> stopId = Id.create(String.format(IdPattern.TRANSIT_STOP, id),
                TransitStopFacility.class);
        TransitStopFacility transitStopFacility = schedule.getFacilities().get(stopId);

        if (transitStopFacility != null) {
            log.warn("TransitStopFacility {} is already existing", stopId);
            return transitStopFacility;
        }

        log.debug("Creating TransitStopFacility {}", stopId);
        transitStopFacility = sf.createTransitStopFacility(stopId, link.getToNode().getCoord(), false);
        transitStopFacility.setLinkId(link.getId());
        schedule.addStopFacility(transitStopFacility);

        return transitStopFacility;
    }

    Node createNode(String id, Coord coord) {
        Id<Node> nodeId = Id.create(String.format(IdPattern.NODE, id), Node.class);
        Node node = network.getNodes().get(nodeId);

        if (node != null) {
            log.warn("Node {} is already existing", nodeId);
            return node;
        }

        log.debug("Creating Node {}", nodeId);
        node = nf.createNode(nodeId, coord);
        network.addNode(node);

        return node;
    }

    Link createLink(LinkType linkType, Node fromNode, Node toNode, double length, Map<String, Object> attributes) {
        Id<Link> linkId = Id.create(String.format(IdPattern.LINK, linkType.getPrefix(), fromNode.getId().toString(),
                toNode.getId().toString()), Link.class);
        Link link = network.getLinks().get(linkId);

        if (link != null) {
            log.warn("Link {} is already existing", linkId);
            return link;
        }

        log.debug("Creating Link {}", linkId);
        link = nf.createLink(linkId, fromNode, toNode);
        link.setAllowedModes(new HashSet<>(List.of(Default.LINK_MODE)));
        link.setLength(length);
        link.setFreespeed(Default.LINK_FREESPEED);
        link.setCapacity(Default.LINK_CAPACITY);
        link.setNumberOfLanes(Default.LINK_LANES);
        putAttributes(link, attributes);
        network.addLink(link);

        return link;
    }

    TransitLine getOrCreateTransitLine(String id) {
        Id<TransitLine> transitLineId = Id.create(String.format(IdPattern.TRANSIT_LINE, id), TransitLine.class);
        TransitLine transitLine = schedule.getTransitLines().get(transitLineId);

        if (transitLine != null) {
            log.debug("TransitLine {} is already existing", transitLineId);
            return transitLine;
        }

        log.debug("Creating TransitLine {}", transitLineId);
        transitLine = sf.createTransitLine(transitLineId);
        schedule.addTransitLine(transitLine);

        return transitLine;
    }

    VehicleType getOrCreateVehicleType(String id, double length, double maxVelocity, int seats, int standingRoom, Map<String, Object> attributes) {
        Id<VehicleType> vehicleTypeId = Id.create(String.format(IdPattern.VEHICLE_TYPE, id), VehicleType.class);
        VehicleType vehicleType = vehicles.getVehicleTypes().get(vehicleTypeId);

        if (vehicleType != null) {
            log.debug("VehicleType {} is already existing", vehicleTypeId);
            return vehicleType;
        }

        log.debug("Creating VehicleType {}", vehicleTypeId);
        vehicleType = vf.createVehicleType(vehicleTypeId);
        vehicleType.getCapacity().setSeats(seats);
        vehicleType.getCapacity().setStandingRoom(standingRoom);
        vehicleType.setMaximumVelocity(maxVelocity);
        vehicleType.setLength(length);
        VehicleUtils.setDoorOperationMode(vehicleType, VehicleType.DoorOperationMode.parallel);
        VehicleUtils.setAccessTime(vehicleType, Default.VEHICLE_ACCESS_TIME);
        VehicleUtils.setEgressTime(vehicleType, Default.VEHICLE_EGRESS_TIME);
        putAttributes(vehicleType, attributes);
        vehicles.addVehicleType(vehicleType);

        return vehicleType;
    }

    Vehicle getOrCreateVehicle(VehicleType vehicleType, String id) {
        Id<Vehicle> vehicleId = Id.create(String.format(IdPattern.VEHICLE, id), Vehicle.class);
        Vehicle vehicle = vehicles.getVehicles().get(vehicleId);

        if (vehicle != null) {
            log.debug("Vehicle {} is already existing", vehicleId);
            return vehicle;
        }

        log.debug("Creating Vehicle {}", vehicleId);
        vehicle = vf.createVehicle(vehicleId, vehicleType);
        vehicles.addVehicle(vehicle);

        return vehicle;
    }

    private static class Default {
        private static final double LINK_FREESPEED = 10000.;
        private static final double LINK_CAPACITY = 10000.;
        private static final double LINK_LANES = 1.;
        private static final String LINK_MODE = "rail";
        private static final double VEHICLE_ACCESS_TIME = 1.;
        private static final double VEHICLE_EGRESS_TIME = 1.;
    }

    private static class IdPattern {
        private static final String DEPARTURE = "%s";
        private static final String TRANSIT_LINE = "%s";
        private static final String TRANSIT_ROUTE = "%s";
        private static final String TRANSIT_STOP = "%s";
        private static final String VEHICLE = "%s";
        private static final String VEHICLE_TYPE = "%s";
        private static final String LINK = "%s_%s-%s";
        private static final String NODE = "%s";
    }

}

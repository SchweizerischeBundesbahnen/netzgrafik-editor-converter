package ch.sbb.pfi.netzgrafikeditor.converter.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.DepartureInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteElement;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RoutePass;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteStop;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitLineInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitRouteInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;
import lombok.extern.slf4j.Slf4j;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MatsimSupplyBuilder implements SupplyBuilder {

    private final Scenario scenario;
    private final InfrastructureRepository infrastructureRepository;
    private final VehicleCircuitsPlanner vehicleCircuitsPlanner;
    private final MatsimSupplyFactory factory;

    private final Map<String, TransitLineInfo> transitLineInfos = new HashMap<>();
    private final Map<String, StopFacilityInfo> stopFacilityInfos = new HashMap<>();
    private final Map<String, TransitRouteContainer> transitRouteContainers = new HashMap<>();

    public MatsimSupplyBuilder(Scenario scenario, InfrastructureRepository infrastructureRepository, VehicleCircuitsPlanner vehicleCircuitsPlanner) {
        this.scenario = scenario;
        this.infrastructureRepository = infrastructureRepository;
        this.vehicleCircuitsPlanner = vehicleCircuitsPlanner;
        factory = new MatsimSupplyFactory(scenario);
    }

    @Override
    public SupplyBuilder addStopFacility(String id, double x, double y) {
        if (stopFacilityInfos.containsKey(id)) {
            throw new RuntimeException("Stop already existing for id " + id);
        }

        stopFacilityInfos.put(id, infrastructureRepository.getStopFacility(id, x, y));

        return this;
    }

    @Override
    public SupplyBuilder addTransitLine(String id, String category) {
        if (transitLineInfos.containsKey(id)) {
            throw new RuntimeException("Transit line already exists for id " + id);
        }

        transitLineInfos.put(id, new TransitLineInfo(id, category));

        return this;
    }

    @Override
    public SupplyBuilder addTransitRoute(String id, String lineId, String originStopId, Duration dwellTimeAtOrigin) {
        TransitLineInfo transitLineInfo = transitLineInfos.get(lineId);
        if (transitLineInfo == null) {
            throw new IllegalArgumentException("Transit line not existing for id " + lineId);
        }

        if (transitRouteContainers.containsKey(id)) {
            throw new IllegalArgumentException("Transit route already existing for id " + id);
        }

        StopFacilityInfo originStop = stopFacilityInfos.get(originStopId);
        if (originStop == null) {
            throw new IllegalArgumentException(
                    String.format("Route stop for line %s at not existing with id %s", lineId, originStopId));
        }

        transitRouteContainers.put(id, new TransitRouteContainer(new TransitRouteInfo(id, transitLineInfo),
                new ArrayList<>(List.of(new RouteStop(originStop, Duration.ZERO, dwellTimeAtOrigin)))));

        return this;
    }

    @Override
    public SupplyBuilder addRouteStop(String routeId, String stopId, Duration travelTime, Duration dwellTime) {
        TransitRouteContainer transitRouteContainer = transitRouteContainers.get(routeId);
        if (transitRouteContainer == null) {
            throw new IllegalArgumentException("Transit route not existing with id " + routeId);
        }

        StopFacilityInfo stopFacilityInfo = stopFacilityInfos.get(stopId);
        if (stopFacilityInfo == null) {
            throw new IllegalArgumentException(
                    String.format("Route stop for route %s at not existing with id %s", routeId, stopId));
        }

        transitRouteContainer.routeElements.add(new RouteStop(stopFacilityInfo, travelTime, dwellTime));

        return this;
    }

    @Override
    public SupplyBuilder addRoutePass(String routeId, String stopId) {
        TransitRouteContainer transitRouteContainer = transitRouteContainers.get(routeId);
        if (transitRouteContainer == null) {
            throw new IllegalArgumentException("Transit route not existing with id " + routeId);
        }

        StopFacilityInfo stopFacilityInfo = stopFacilityInfos.get(stopId);
        if (stopFacilityInfo == null) {
            throw new IllegalArgumentException(
                    String.format("Route pass for line %s at not existing with id %s", routeId, stopId));
        }

        transitRouteContainer.routeElements.add(new RoutePass(stopFacilityInfo));

        return this;
    }

    @Override
    public SupplyBuilder addDeparture(String routeId, LocalTime time) {
        TransitRouteContainer transitRouteContainer = transitRouteContainers.get(routeId);
        if (transitRouteContainer == null) {
            throw new IllegalArgumentException("Transit route not existing with id " + routeId);
        }

        // TODO: Add check that all departures are allocated with a vehicle by the circuits planner
        DepartureInfo departureInfo = new DepartureInfo(transitRouteContainer.transitRouteInfo, time);
        vehicleCircuitsPlanner.register(departureInfo);

        return this;
    }

    @Override
    public void build() {

        InfrastructureBuilder infrastructureBuilder = new InfrastructureBuilder(scenario, factory,
                infrastructureRepository);

        for (StopFacilityInfo stopFacilityInfo : stopFacilityInfos.values()) {
            infrastructureBuilder.buildTransitStopFacility(stopFacilityInfo);
        }

        // build transit routes
        Map<String, TransitRoute> transitRoutes = new HashMap<>();
        for (TransitRouteContainer container : transitRouteContainers.values()) {
            transitRoutes.put(container.transitRouteInfo.getId(),
                    infrastructureBuilder.buildTransitRoute(container.transitRouteInfo, container.routeElements));
        }

        // add departures
        List<VehicleAllocation> vehicleAllocations = vehicleCircuitsPlanner.plan();

        if (vehicleAllocations.isEmpty()) {
            throw new IllegalStateException("No vehicle allocations found.");
        }

        for (VehicleAllocation vehicleAllocation : vehicleAllocations) {

            log.debug("Adding departure {} (line: {} route: {}, vehicle type: {} vehicle: {})",
                    vehicleAllocation.getDepartureId(),
                    vehicleAllocation.getDepartureInfo().getTransitRouteInfo().getTransitLineInfo().getId(),
                    vehicleAllocation.getDepartureInfo().getTransitRouteInfo().getId(),
                    vehicleAllocation.getVehicleInfo().getVehicleTypeInfo().getId(),
                    vehicleAllocation.getVehicleInfo().getId());

            // vehicle
            VehicleTypeInfo vehicleTypeInfo = vehicleAllocation.getVehicleInfo().getVehicleTypeInfo();
            VehicleType vehicleType = factory.getOrCreateVehicleType(vehicleTypeInfo.getId(),
                    vehicleTypeInfo.getLength(), vehicleTypeInfo.getMaxVelocity(), vehicleTypeInfo.getCapacity(),
                    vehicleTypeInfo.getAttributes());
            Vehicle vehicle = factory.getOrCreateVehicle(vehicleType, vehicleAllocation.getVehicleInfo().getId());

            // departures
            DepartureInfo departureInfo = vehicleAllocation.getDepartureInfo();
            Departure departure = factory.createDeparture(vehicleAllocation.getDepartureId(),
                    departureInfo.getTime().toSecondOfDay());
            departure.setVehicleId(vehicle.getId());

            // add departure to the matching transit route
            transitRoutes.get(departureInfo.getTransitRouteInfo().getId()).addDeparture(departure);
        }
    }

    record TransitRouteContainer(TransitRouteInfo transitRouteInfo, List<RouteElement> routeElements) {
    }

}

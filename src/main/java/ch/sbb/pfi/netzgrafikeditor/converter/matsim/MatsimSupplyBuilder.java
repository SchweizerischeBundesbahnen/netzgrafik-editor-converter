package ch.sbb.pfi.netzgrafikeditor.converter.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.supply.DepartureInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.RouteDirection;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.RouteElement;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.RoutePass;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.RouteStop;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.TransitLineInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.VehicleTypeInfo;
import lombok.extern.slf4j.Slf4j;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MatsimSupplyBuilder implements SupplyBuilder {

    private final Scenario scenario;
    private final InfrastructureRepository infrastructureRepository;
    private final RollingStockRepository rollingStockRepository;
    private final VehicleCircuitsPlanner vehicleCircuitsPlanner;
    private final MatsimSupplyFactory factory;

    private final Map<String, StopFacilityInfo> stopFacilityInfos = new HashMap<>();
    private final Map<String, TransitLineContainer> transitLineContainers = new HashMap<>();
    private final Map<String, VehicleTypeInfo> vehicleTypeInfos = new HashMap<>();

    public MatsimSupplyBuilder(Scenario scenario, InfrastructureRepository infrastructureRepository, RollingStockRepository rollingStockRepository, VehicleCircuitsPlanner vehicleCircuitsPlanner) {
        this.scenario = scenario;
        this.infrastructureRepository = infrastructureRepository;
        this.rollingStockRepository = rollingStockRepository;
        this.vehicleCircuitsPlanner = vehicleCircuitsPlanner;
        factory = new MatsimSupplyFactory(scenario);
    }

    @Override
    public SupplyBuilder addStopFacility(String id) {
        if (stopFacilityInfos.containsKey(id)) {
            throw new RuntimeException("Stop already existing for id " + id);
        }

        stopFacilityInfos.put(id, infrastructureRepository.getStopFacility(id));

        return this;
    }

    @Override
    public SupplyBuilder addTransitLine(String lineId, String vehicleTypeId, String originStopId, Duration dwellTimeAtOrigin) {
        if (transitLineContainers.containsKey(lineId)) {
            throw new IllegalArgumentException("Transit line already existing for id " + lineId);
        }

        VehicleTypeInfo vehicleTypeInfo = vehicleTypeInfos.get(vehicleTypeId);
        if (vehicleTypeInfo == null) {
            vehicleTypeInfo = rollingStockRepository.getVehicleType(vehicleTypeId);
            vehicleTypeInfos.put(vehicleTypeId, vehicleTypeInfo);
        }

        StopFacilityInfo originStop = stopFacilityInfos.get(originStopId);
        if (originStop == null) {
            throw new IllegalArgumentException(
                    String.format("Route stop for line %s at not existing with id %s", lineId, originStopId));
        }

        transitLineContainers.put(lineId, new TransitLineContainer(new TransitLineInfo(lineId, vehicleTypeInfo),
                new ArrayList<>(List.of(new RouteStop(originStop, Duration.ZERO, dwellTimeAtOrigin))),
                new EnumMap<>(RouteDirection.class)));

        return this;
    }

    @Override
    public SupplyBuilder addRouteStop(String lineId, String stopId, Duration travelTime, Duration dwellTime) {
        TransitLineContainer transitLineContainer = transitLineContainers.get(lineId);
        if (transitLineContainer == null) {
            throw new IllegalArgumentException("Transit line not existing with id " + lineId);
        }

        StopFacilityInfo stopFacilityInfo = stopFacilityInfos.get(stopId);
        if (stopFacilityInfo == null) {
            throw new IllegalArgumentException(
                    String.format("Route stop for line %s at not existing with id %s", lineId, stopId));
        }

        transitLineContainer.routeElements.add(new RouteStop(stopFacilityInfo, travelTime, dwellTime));

        return this;
    }

    @Override
    public SupplyBuilder addRoutePass(String lineId, String stopId) {
        TransitLineContainer transitLineContainer = transitLineContainers.get(lineId);
        if (transitLineContainer == null) {
            throw new IllegalArgumentException("Transit line not existing with id " + lineId);
        }

        StopFacilityInfo stopFacilityInfo = stopFacilityInfos.get(stopId);
        if (stopFacilityInfo == null) {
            throw new IllegalArgumentException(
                    String.format("Route pass for line %s at not existing with id %s", lineId, stopId));
        }

        transitLineContainer.routeElements.add(new RoutePass(stopFacilityInfo));

        return this;
    }

    @Override
    public SupplyBuilder addDeparture(String lineId, RouteDirection direction, LocalTime time) {
        TransitLineContainer transitLineContainer = transitLineContainers.get(lineId);
        if (transitLineContainer == null) {
            throw new IllegalArgumentException("Transit line not existing with id " + lineId);
        }

        DepartureInfo departure = new DepartureInfo(transitLineContainer.transitLineInfo, direction, time);

        List<DepartureInfo> departures = transitLineContainer.departures.computeIfAbsent(direction,
                k -> new ArrayList<>());
        departures.add(departure);

        vehicleCircuitsPlanner.register(departure);

        return this;
    }

    @Override
    public void build() {

        InfrastructureBuilder infrastructureBuilder = new InfrastructureBuilder(scenario, factory,
                infrastructureRepository);

        for (StopFacilityInfo stopFacilityInfo : stopFacilityInfos.values()) {
            infrastructureBuilder.buildTransitStopFacility(stopFacilityInfo);
        }

        // build transit lines with corresponding transit routes
        EnumMap<RouteDirection, Map<String, TransitRoute>> transitRoutes = new EnumMap<>(RouteDirection.class);
        for (TransitLineContainer container : transitLineContainers.values()) {
            // forward direction
            transitRoutes.computeIfAbsent(RouteDirection.FORWARD, k -> new HashMap<>())
                    .put(container.transitLineInfo.getId(),
                            infrastructureBuilder.buildTransitRoute(container.transitLineInfo, container.routeElements,
                                    RouteDirection.FORWARD));

            // reverse direction
            if (container.hasReverseDepartures()) {
                transitRoutes.computeIfAbsent(RouteDirection.REVERSE, k -> new HashMap<>())
                        .put(container.transitLineInfo.getId(),
                                infrastructureBuilder.buildTransitRoute(container.transitLineInfo,
                                        container.routeElements, RouteDirection.REVERSE));
            }
        }

        // add departures
        List<VehicleAllocation> vehicleAllocations = vehicleCircuitsPlanner.plan();

        if (vehicleAllocations.isEmpty()) {
            throw new IllegalStateException("No vehicle allocations found.");
        }

        for (VehicleAllocation vehicleAllocation : vehicleAllocations) {

            log.debug("Adding departure {} (line: {} route: {}, vehicle type: {} vehicle: {})",
                    vehicleAllocation.getDepartureId(), "none", "none",
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
            transitRoutes.get(departureInfo.getDirection())
                    .get(departureInfo.getTransitLine().getId())
                    .addDeparture(departure);
        }
    }

    record TransitLineContainer(TransitLineInfo transitLineInfo, List<RouteElement> routeElements,
                                EnumMap<RouteDirection, List<DepartureInfo>> departures) {

        boolean hasReverseDepartures() {
            return departures.containsKey(RouteDirection.REVERSE) && !departures.get(RouteDirection.REVERSE).isEmpty();
        }
    }

}

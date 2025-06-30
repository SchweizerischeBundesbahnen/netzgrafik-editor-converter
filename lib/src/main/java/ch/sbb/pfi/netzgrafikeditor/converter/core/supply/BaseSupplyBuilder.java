package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class BaseSupplyBuilder<T> implements SupplyBuilder<T> {

    private final InfrastructureRepository infrastructureRepository;
    private final RollingStockRepository rollingStockRepository;
    private final VehicleCircuitsPlanner vehicleCircuitsPlanner;

    private final Map<String, TransitLineInfo> transitLineInfos = new HashMap<>();
    private final Map<String, StopFacilityInfo> stopFacilityInfos = new HashMap<>();
    private final Map<String, TransitRouteContainer> transitRouteContainers = new HashMap<>();

    public BaseSupplyBuilder(InfrastructureRepository infrastructureRepository, RollingStockRepository rollingStockRepository, VehicleCircuitsPlanner vehicleCircuitsPlanner) {
        this.infrastructureRepository = infrastructureRepository;
        this.rollingStockRepository = rollingStockRepository;
        this.vehicleCircuitsPlanner = vehicleCircuitsPlanner;
    }

    @Override
    public SupplyBuilder<T> addStopFacility(String id, String name, double x, double y) {
        if (stopFacilityInfos.containsKey(id)) {
            throw new IllegalArgumentException("Stop already existing for id " + id);
        }

        StopFacilityInfo stopFacilityInfo = infrastructureRepository.getStopFacility(id, name, x, y);
        if (stopFacilityInfo == null) {
            throw new IllegalStateException("Stop with id " + id + " does not exist in infrastructure repository");
        }

        stopFacilityInfos.put(id, stopFacilityInfo);

        return this;
    }

    @Override
    public SupplyBuilder<T> addTransitLine(String id, String category) {
        if (transitLineInfos.containsKey(id)) {
            throw new IllegalArgumentException("Transit line already exists for id " + id);
        }

        VehicleTypeInfo vehicleTypeInfo = rollingStockRepository.getVehicleType(category);
        if (vehicleTypeInfo == null) {
            throw new IllegalStateException("No vehicle type found in RollingStockRepository for category " + category);
        }

        transitLineInfos.put(id, new TransitLineInfo(id, category, vehicleTypeInfo.getTransportMode()));

        return this;
    }

    @Override
    public SupplyBuilder<T> addTransitRoute(String id, String lineId, String originStopId, Duration dwellTimeAtOrigin) {
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
    public SupplyBuilder<T> addRouteStop(String routeId, String stopId, Duration travelTime, Duration dwellTime) {
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
    public SupplyBuilder<T> addRoutePass(String routeId, String stopId) {
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
    public SupplyBuilder<T> addDeparture(String routeId, ServiceDayTime time) {
        TransitRouteContainer transitRouteContainer = transitRouteContainers.get(routeId);
        if (transitRouteContainer == null) {
            throw new IllegalArgumentException("Transit route not existing with id " + routeId);
        }

        DepartureInfo departureInfo = new DepartureInfo(transitRouteContainer.transitRouteInfo, time);
        vehicleCircuitsPlanner.register(departureInfo);

        return this;
    }

    @Override
    public T build() {

        for (StopFacilityInfo stopFacilityInfo : stopFacilityInfos.values()) {
            buildStopFacility(stopFacilityInfo);
        }

        for (TransitRouteContainer container : transitRouteContainers.values()) {
            buildTransitRoute(container);
        }

        List<VehicleAllocation> vehicleAllocations = vehicleCircuitsPlanner.plan();

        if (vehicleAllocations == null || vehicleAllocations.isEmpty()) {
            throw new IllegalStateException("No vehicle allocations received from vehicle circuit planer");
        }
        validateAllocations(vehicleAllocations);

        for (VehicleAllocation vehicleAllocation : vehicleAllocations) {
            buildDeparture(vehicleAllocation);
        }

        return getResult();
    }

    private void validateAllocations(List<VehicleAllocation> vehicleAllocations) {
        for (VehicleAllocation vehicleAllocation : vehicleAllocations) {
            TransportMode expectedMode = vehicleAllocation.getDepartureInfo()
                    .getTransitRouteInfo()
                    .getTransitLineInfo()
                    .getTransportMode();
            TransportMode actualMode = vehicleAllocation.getVehicleInfo().getVehicleTypeInfo().getTransportMode();

            if (expectedMode != actualMode) {
                throw new IllegalStateException(String.format(
                        "Inconsistent transport mode ('%s' / '%s') set on vehicle allocation for departure '%s' on route '%s' and line '%s'",
                        expectedMode, actualMode, vehicleAllocation.getDepartureId(),
                        vehicleAllocation.getDepartureInfo().getTransitRouteInfo().getId(),
                        vehicleAllocation.getDepartureInfo().getTransitRouteInfo().getTransitLineInfo().getId()));
            }
        }
    }

    protected abstract void buildStopFacility(StopFacilityInfo stopFacilityInfo);

    protected abstract void buildTransitRoute(TransitRouteContainer transitRouteContainer);

    protected abstract void buildDeparture(VehicleAllocation vehicleAllocation);

    protected abstract T getResult();

    protected record TransitRouteContainer(TransitRouteInfo transitRouteInfo, List<RouteElement> routeElements) {
    }
}

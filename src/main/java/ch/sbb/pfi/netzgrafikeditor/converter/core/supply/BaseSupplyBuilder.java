package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public abstract class BaseSupplyBuilder implements SupplyBuilder {

    private final InfrastructureRepository infrastructureRepository;
    private final VehicleCircuitsPlanner vehicleCircuitsPlanner;

    private final Map<String, TransitLineInfo> transitLineInfos = new HashMap<>();
    private final Map<String, StopFacilityInfo> stopFacilityInfos = new HashMap<>();
    private final Map<String, TransitRouteContainer> transitRouteContainers = new HashMap<>();

    public BaseSupplyBuilder(InfrastructureRepository infrastructureRepository, VehicleCircuitsPlanner vehicleCircuitsPlanner) {
        this.infrastructureRepository = infrastructureRepository;
        this.vehicleCircuitsPlanner = vehicleCircuitsPlanner;
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

        DepartureInfo departureInfo = new DepartureInfo(transitRouteContainer.transitRouteInfo, time);
        vehicleCircuitsPlanner.register(departureInfo);

        return this;
    }

    @Override
    public void build() {
        addStopFacilities(stopFacilityInfos);
        addTransitRoutes(transitRouteContainers);
        addDepartures(vehicleCircuitsPlanner.plan());
    }

    protected abstract void addStopFacilities(Map<String, StopFacilityInfo> stopFacilityInfos);

    protected abstract void addTransitRoutes(Map<String, TransitRouteContainer> transitRouteContainers);

    protected abstract void addDepartures(List<VehicleAllocation> plan);

    protected record TransitRouteContainer(TransitRouteInfo transitRouteInfo, List<RouteElement> routeElements) {
    }

}

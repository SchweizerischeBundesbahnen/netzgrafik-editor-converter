package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import java.time.Duration;
import java.time.LocalTime;

public interface SupplyBuilder {

    SupplyBuilder addStopFacility(String id, double x, double y);

    SupplyBuilder addTransitLine(String id, String productId);

    SupplyBuilder addTransitRoute(String id, String lineId, String originStopId, Duration dwellTimeAtOrigin);

    SupplyBuilder addRouteStop(String routeId, String stopId, Duration travelTime, Duration dwellTime);

    SupplyBuilder addRoutePass(String routeId, String stopId);

    SupplyBuilder addDeparture(String routeId, LocalTime time);

    void build();

}
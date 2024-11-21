package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import java.time.Duration;
import java.time.LocalTime;

public interface SupplyBuilder<T> {

    SupplyBuilder<T> addStopFacility(String id, double x, double y);

    SupplyBuilder<T> addTransitLine(String id, String category);

    SupplyBuilder<T> addTransitRoute(String id, String lineId, String originStopId, Duration dwellTimeAtOrigin);

    SupplyBuilder<T> addRouteStop(String routeId, String stopId, Duration travelTime, Duration dwellTime);

    SupplyBuilder<T> addRoutePass(String routeId, String stopId);

    SupplyBuilder<T> addDeparture(String routeId, LocalTime time);

    T build();

}
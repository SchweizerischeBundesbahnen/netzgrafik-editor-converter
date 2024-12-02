package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;

import java.time.Duration;

public interface SupplyBuilder<T> {

    SupplyBuilder<T> addStopFacility(String id, double x, double y);

    SupplyBuilder<T> addTransitLine(String id, String category);

    SupplyBuilder<T> addTransitRoute(String id, String lineId, String originStopId, Duration dwellTimeAtOrigin);

    SupplyBuilder<T> addRouteStop(String routeId, String stopId, Duration travelTime, Duration dwellTime);

    SupplyBuilder<T> addRoutePass(String routeId, String stopId);

    SupplyBuilder<T> addDeparture(String routeId, ServiceDayTime time);

    T build();

}
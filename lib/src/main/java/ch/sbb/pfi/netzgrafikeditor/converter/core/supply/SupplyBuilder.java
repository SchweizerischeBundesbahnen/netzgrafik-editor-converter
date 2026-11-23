package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;

import java.time.Duration;

public interface SupplyBuilder<T> {

    SupplyBuilder<T> addStopFacility(String id, String name, double x, double y);

    SupplyBuilder<T> addTransitLine(String id, String category);

    SupplyBuilder<T> addTransitRoute(String id, String lineId, String originStopId, Duration dwellTimeAtOrigin, StopAccess access);

    SupplyBuilder<T> addRouteStop(String routeId, String stopId, Duration travelTime, Duration dwellTime, StopAccess access);

    SupplyBuilder<T> addRoutePass(String routeId, String stopId);

    SupplyBuilder<T> addDeparture(String routeId, ServiceDayTime time);

    /**
     * Adds a connection at a stop from one departure to another, each identified by its transit route and its time at
     * the first stop of the route; see {@link ConnectionInfo}.
     */
    SupplyBuilder<T> addConnection(String stopId, String fromRouteId, ServiceDayTime fromDeparture, String toRouteId, ServiceDayTime toDeparture);

    /**
     * Forbids transfers at a stop, except at the connections added for it.
     */
    SupplyBuilder<T> forbidTransfers(String stopId);

    T build();

}

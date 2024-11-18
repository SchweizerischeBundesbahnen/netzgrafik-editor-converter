package ch.sbb.pfi.netzgrafikeditor.converter.supply;

import java.time.Duration;
import java.time.LocalTime;

public interface SupplyBuilder {

    SupplyBuilder addStopFacility(String id, double x, double y);

    SupplyBuilder addTransitLine(String lineId, String vehicleTypeId, String originStopId, Duration dwellTimeAtOrigin);

    SupplyBuilder addRouteStop(String lineId, String stopId, Duration travelTime, Duration dwellTime);

    SupplyBuilder addRoutePass(String lineId, String stopId);

    SupplyBuilder addDeparture(String lineId, RouteDirection direction, LocalTime time);

    void build();

}
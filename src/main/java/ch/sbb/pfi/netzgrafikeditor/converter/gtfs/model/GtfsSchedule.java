package ch.sbb.pfi.netzgrafikeditor.converter.gtfs.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GtfsSchedule {

    List<Agency> agencies;

    List<Stop> stops;

    List<Route> routes;

    List<Trip> trips;

    List<StopTime> stopTimes;

    List<Calendar> calendars;

}

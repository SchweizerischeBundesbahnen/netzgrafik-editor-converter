package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class GtfsSchedule {

    @Builder.Default
    FeedInfo feedInfo = FeedInfo.builder().build();

    @Builder.Default
    List<Agency> agencies = List.of(Agency.builder().build());

    List<Stop> stops;

    List<Route> routes;

    List<Trip> trips;

    List<StopTime> stopTimes;

    @Builder.Default
    List<Calendar> calendars = List.of(Calendar.builder().build());

}

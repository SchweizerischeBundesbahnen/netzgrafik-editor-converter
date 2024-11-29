package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

@Value
@Builder
public class StopTime {

    String tripId;

    LocalTime arrivalTime;

    LocalTime departureTime;

    String stopId;

    int stopSequence;

}

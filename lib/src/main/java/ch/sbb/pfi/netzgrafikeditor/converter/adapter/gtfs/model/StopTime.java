package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StopTime {

    String tripId;

    ServiceDayTime arrivalTime;

    ServiceDayTime departureTime;

    String stopId;

    int stopSequence;

}

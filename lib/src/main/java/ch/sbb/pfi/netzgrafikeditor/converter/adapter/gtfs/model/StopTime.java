package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StopTime {

    /**
     * Value of pickup_type and drop_off_type where no pickup or drop off is available; empty means regularly
     * scheduled.
     */
    public static final int NOT_AVAILABLE = 1;

    String tripId;

    ServiceDayTime arrivalTime;

    ServiceDayTime departureTime;

    String stopId;

    int stopSequence;

    Integer pickupType;

    Integer dropOffType;

}

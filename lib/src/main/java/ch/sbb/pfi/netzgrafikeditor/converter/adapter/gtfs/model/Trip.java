package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Trip {

    String routeId;

    String serviceId;

    String tripId;

    String tripHeadsign;

}

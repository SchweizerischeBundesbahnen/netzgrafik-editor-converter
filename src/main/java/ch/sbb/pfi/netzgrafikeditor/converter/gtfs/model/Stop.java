package ch.sbb.pfi.netzgrafikeditor.converter.gtfs.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Stop {

    String stopId;

    String stopName;

    double stopLat;

    double stopLon;

}

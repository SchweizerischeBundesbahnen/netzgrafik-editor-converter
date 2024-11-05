package ch.sbb.pfi.netzgrafikeditor.converter.supply;

import lombok.Value;

import java.time.LocalTime;

@Value
public class DepartureInfo {

    TransitLineInfo transitLine;
    RouteDirection direction;
    LocalTime time;

}

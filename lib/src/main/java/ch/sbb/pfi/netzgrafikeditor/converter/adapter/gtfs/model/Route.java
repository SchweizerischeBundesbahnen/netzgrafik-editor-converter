package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Route {

    String routeId;

    String agencyId;

    String routeShortName;

    String routeLongName;

    RouteType routeType;

}

package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import lombok.Value;

@Value
public class DepartureInfo {

    TransitRouteInfo transitRouteInfo;
    ServiceDayTime time;

}

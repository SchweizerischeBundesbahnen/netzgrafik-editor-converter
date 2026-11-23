package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import lombok.Value;

/**
 * A connection at a stop from one departure to another: the departing trip waits for the arriving one, and passengers
 * can change in no time.
 * <p>
 * A departure is identified by its transit route and its time at the first stop of that route, as given to
 * {@link SupplyBuilder#addDeparture}.
 */
@Value
public class ConnectionInfo {

    StopFacilityInfo stopFacilityInfo;
    TransitRouteInfo fromTransitRouteInfo;
    ServiceDayTime fromDeparture;
    TransitRouteInfo toTransitRouteInfo;
    ServiceDayTime toDeparture;

}

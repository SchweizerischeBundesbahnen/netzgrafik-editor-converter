package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import lombok.Value;

import java.time.Duration;

@Value
public class RouteStop implements RouteElement {

    StopFacilityInfo stopFacilityInfo;
    Duration travelTime;
    Duration dwellTime;

    @Override
    public void accept(RouteElementVisitor visitor) {
        visitor.visit(this);
    }

}

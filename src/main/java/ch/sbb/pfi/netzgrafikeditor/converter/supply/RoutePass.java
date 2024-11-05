package ch.sbb.pfi.netzgrafikeditor.converter.supply;

import lombok.Value;

@Value
public class RoutePass implements RouteElement {

    StopFacilityInfo stopFacilityInfo;

    @Override
    public void accept(RouteElementVisitor visitor) {
        visitor.visit(this);
    }

}

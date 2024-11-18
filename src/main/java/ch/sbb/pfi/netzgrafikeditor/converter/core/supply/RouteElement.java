package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

public interface RouteElement {

    StopFacilityInfo getStopFacilityInfo();

    void accept(RouteElementVisitor visitor);

}

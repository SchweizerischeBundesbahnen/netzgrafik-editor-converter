package ch.sbb.pfi.netzgrafikeditor.converter.supply;

public interface RouteElement {

    StopFacilityInfo getStopFacilityInfo();

    void accept(RouteElementVisitor visitor);

}

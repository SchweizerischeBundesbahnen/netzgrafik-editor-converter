package ch.sbb.pfi.netzgrafikeditor.converter.supply;

import java.util.List;

public interface VehicleCircuitsPlanner {

    void register(DepartureInfo departureInfo);

    List<VehicleAllocation> plan();

}

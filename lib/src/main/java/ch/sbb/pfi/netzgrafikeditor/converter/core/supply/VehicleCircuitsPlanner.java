package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import java.util.List;

public interface VehicleCircuitsPlanner {

    void register(DepartureInfo departureInfo);

    List<VehicleAllocation> plan();

}

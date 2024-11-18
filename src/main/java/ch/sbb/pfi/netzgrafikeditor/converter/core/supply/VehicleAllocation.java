package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import lombok.Value;

@Value
public class VehicleAllocation {

    String departureId;
    DepartureInfo departureInfo;
    VehicleInfo vehicleInfo;

}

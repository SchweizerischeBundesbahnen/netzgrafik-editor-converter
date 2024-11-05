package ch.sbb.pfi.netzgrafikeditor.converter.supply;

import lombok.Value;

@Value
public class VehicleAllocation {

    String departureId;
    DepartureInfo departureInfo;
    VehicleInfo vehicleInfo;

}

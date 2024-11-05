package ch.sbb.pfi.netzgrafikeditor.converter.supply;

import lombok.Value;

import java.time.Duration;

@Value
public class TransitLineInfo {

    String id;
    VehicleTypeInfo vehicleTypeInfo;
    StopFacilityInfo originStop;
    Duration dwellTimeAtOrigin;

}

package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import lombok.Value;

import java.util.Map;


@Value
public class VehicleTypeInfo {

    String id;
    int seats;
    int standingRoom;
    double length;
    double maxVelocity;
    Map<String, Object> attributes;

}

package ch.sbb.pfi.netzgrafikeditor.converter.supply;

import lombok.Value;

import java.util.Map;


@Value
public class VehicleTypeInfo {

    String id;
    int capacity;
    double length;
    double maxVelocity;
    Map<String, Object> attributes;

}

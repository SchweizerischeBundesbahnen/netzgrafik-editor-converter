package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import lombok.Value;

import java.util.HashMap;
import java.util.Map;

@Value
public class StopFacilityInfo {

    String id;
    Coordinate coordinate;
    Map<String, Object> linkAttributes = new HashMap<>();

}

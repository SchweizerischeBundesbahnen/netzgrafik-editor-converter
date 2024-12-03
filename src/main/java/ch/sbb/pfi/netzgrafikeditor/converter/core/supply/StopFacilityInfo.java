package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.Coordinate;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

@Value
public class StopFacilityInfo {

    String id;
    Coordinate coordinate;
    Map<String, Object> attributes = new HashMap<>();

}

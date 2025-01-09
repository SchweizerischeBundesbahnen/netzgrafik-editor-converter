package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.Coordinate;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

@Value
public class TrackSegmentInfo {

    String segmentId;
    Coordinate fromCoordinate;
    Coordinate toCoordinate;
    double length;
    Map<String, Object> linkAttributes = new HashMap<>();

}

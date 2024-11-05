package ch.sbb.pfi.netzgrafikeditor.converter.supply;

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

package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import lombok.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * A segment of track from one node of the track infrastructure to another.
 * <p>
 * A segment id always connects the same two nodes.
 */
@Value
public class TrackSegmentInfo {

    String segmentId;
    TrackNodeInfo fromNode;
    TrackNodeInfo toNode;
    double length;
    Map<String, Object> linkAttributes = new HashMap<>();

}

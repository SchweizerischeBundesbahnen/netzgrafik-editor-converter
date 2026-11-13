package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.Coordinate;
import lombok.Value;

/**
 * A node of the track infrastructure: a stop, or a junction where track segments meet.
 * <p>
 * The node of a stop has the id of the stop. Segments that meet share the id of the node they meet at, which makes them
 * one continuous track.
 */
@Value
public class TrackNodeInfo {

    String id;
    Coordinate coordinate;

}

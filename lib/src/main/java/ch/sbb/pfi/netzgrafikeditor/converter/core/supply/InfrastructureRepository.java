package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import java.util.List;

/**
 * Provides the stop facilities and the tracks between them.
 */
public interface InfrastructureRepository {

    StopFacilityInfo getStopFacility(String stopId, String stopName, double x, double y);

    /**
     * Gets the track between two consecutive nodes of a transit route, stopped at or passed, as its segments in the
     * order they are travelled.
     * <p>
     * The track forms one continuous path from the node of the from stop to the node of the to stop:
     * <ul>
     *     <li>the first segment starts at the node of the from stop, and the last ends at the node of the to stop; the
     *     node of a stop has the id of the stop,</li>
     *     <li>each segment starts at the node the segment before ends at, so consecutive segments share the id of the
     *     junction between them,</li>
     *     <li>a segment id always stands for the same segment: the same two nodes, the same length and the same link
     *     attributes,</li>
     *     <li>a node id always stands at the same coordinate.</li>
     * </ul>
     * A track that breaks these rules is handled according to the validation strategy of the supply builder that
     * reads it.
     */
    List<TrackSegmentInfo> getTrack(StopFacilityInfo fromStop, StopFacilityInfo toStop, TransitRouteInfo transitRouteInfo);

}

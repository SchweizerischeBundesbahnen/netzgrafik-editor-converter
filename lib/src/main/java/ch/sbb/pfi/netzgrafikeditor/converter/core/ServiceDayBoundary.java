package ch.sbb.pfi.netzgrafikeditor.converter.core;

/**
 * Handling of trips at the boundaries of the service day.
 * <p>
 * The network graphic describes one pattern hour, which is rolled out over the service day. The boundary strategy
 * defines which trips are created at the start and end of the service day. It only applies to trainruns that operate
 * the whole service day; trainruns with day time intervals (e.g. peak hours) always run full trips.
 */
public enum ServiceDayBoundary {

    /**
     * Trips run on their full route and are created if they depart from their first stop within the service day.
     */
    FULL,

    /**
     * Trips are clipped to the service day: They start at the first stop departing at or after the service day start
     * and end at the last stop arriving at or before the service day end. Trips with less than two stops within the
     * service day are dropped.
     */
    CLIP,

    /**
     * Like {@link #CLIP}, but trips only start and end at depots: They start at the first depot departing at or after
     * the service day start and end at the last depot arriving at or before the service day end. Depots are the nodes
     * with the configured depot label; line termini always count as depots.
     */
    DEPOT

}

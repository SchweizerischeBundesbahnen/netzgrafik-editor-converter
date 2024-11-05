package ch.sbb.pfi.netzgrafikeditor.converter.matsim;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
enum LinkType {

    /**
     * Links inside the stations.
     */
    STOP("stop_link"),

    /**
     * Links in the depot and connecting the depot.
     */
    DEPOT("depot_link"),

    /**
     * Links on the route between two stations.
     */
    ROUTE("track_link");

    private final String prefix;

}

package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.Builder;
import lombok.Value;

/**
 * A row of the GTFS transfers.txt; the route and trip ids narrow a transfer between two stops to particular routes or
 * trips, and are empty otherwise.
 */
@Value
@Builder
public class Transfer {

    String fromStopId;

    String toStopId;

    String fromRouteId;

    String toRouteId;

    String fromTripId;

    String toTripId;

    TransferType transferType;

    Integer minTransferTime;

}

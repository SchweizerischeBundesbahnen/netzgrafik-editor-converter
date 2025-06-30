package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import lombok.Value;

/**
 * A specific path or pattern of a {@link TransitLineInfo}, defined by a sequence of stops and passes.
 */
@Value
public class TransitRouteInfo {

    String id;
    TransitLineInfo transitLineInfo;

}

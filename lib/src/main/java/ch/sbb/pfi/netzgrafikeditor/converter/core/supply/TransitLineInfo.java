package ch.sbb.pfi.netzgrafikeditor.converter.core.supply;

import lombok.Value;


/**
 * Represents a group of routes publicly identified by a common name (e.g., "S1", "IC 5").
 * <p>
 * The {@link TransportMode} is defined at this level to ensure all associated routes share the same fundamental mode.
 * For exceptions (e.g., a bus replacing a tram), model the replacement service as a separate, new
 * {@code TransitLineInfo}.
 */
@Value
public class TransitLineInfo {

    String id;
    String category;
    TransportMode transportMode;

}

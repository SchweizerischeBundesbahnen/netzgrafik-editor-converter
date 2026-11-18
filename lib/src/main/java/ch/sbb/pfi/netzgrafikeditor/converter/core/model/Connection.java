package ch.sbb.pfi.netzgrafikeditor.converter.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * A connection drawn in a node of the network graphic, between the ports of two trainruns.
 * <p>
 * As in the Netzgrafik-Editor, the trainrun at port 1 arrives first and the trainrun at port 2 waits for it.
 */
@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Connection implements Identifiable {

    int id;

    int port1Id;

    int port2Id;

}

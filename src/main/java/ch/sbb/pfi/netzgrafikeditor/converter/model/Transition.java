package ch.sbb.pfi.netzgrafikeditor.converter.model;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;


@Value
@Builder
@Jacksonized
public class Transition implements Identifiable {

    int id;

    int port1Id;

    int port2Id;

    boolean isNonStopTransit;

}
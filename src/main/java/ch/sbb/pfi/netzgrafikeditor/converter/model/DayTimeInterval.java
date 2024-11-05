package ch.sbb.pfi.netzgrafikeditor.converter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;


@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class DayTimeInterval {

    int to;

    int from;

}

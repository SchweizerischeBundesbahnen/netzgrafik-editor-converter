package ch.sbb.pfi.netzgrafikeditor.converter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;


@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Time {

    boolean lock;

    double time;

    double consecutiveTime;

}

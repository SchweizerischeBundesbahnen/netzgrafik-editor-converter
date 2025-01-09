package ch.sbb.pfi.netzgrafikeditor.converter.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;


@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrainrunTimeCategory implements Identifiable {

    int id;

    String shortName;

    String name;

    @JsonProperty("dayTimeInterval")
    List<DayTimeInterval> dayTimeIntervals;

}

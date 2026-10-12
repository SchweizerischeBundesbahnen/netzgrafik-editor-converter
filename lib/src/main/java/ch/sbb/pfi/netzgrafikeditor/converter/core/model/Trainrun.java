package ch.sbb.pfi.netzgrafikeditor.converter.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;


@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Trainrun implements Identifiable {

    int id;

    String name;

    int categoryId;

    int frequencyId;

    int trainrunTimeCategoryId;

    // older network graphics have no direction, the NGE treats them as round trips
    @Builder.Default
    Direction direction = Direction.ROUND_TRIP;

    public enum Direction {
        @JsonProperty("round_trip")
        ROUND_TRIP,
        @JsonProperty("one_way")
        ONE_WAY
    }

}

package ch.sbb.pfi.netzgrafikeditor.converter.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;


@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrainrunSection implements Identifiable {

    int id;

    int sourceNodeId;

    int targetNodeId;

    int trainrunId;

    Time sourceArrival;

    Time travelTime;

    Time backwardTravelTime;

    Time sourceDeparture;

    Time targetArrival;

    Time targetDeparture;

    // older network graphics have no backward travel time, the NGE treats them as symmetric
    public Time getBackwardTravelTime() {
        return backwardTravelTime != null ? backwardTravelTime : travelTime;
    }

}

package ch.sbb.pfi.netzgrafikeditor.converter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;


@Getter
@Setter
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrainrunSection implements Identifiable {

    private int id;

    private int sourceNodeId;

    private int targetNodeId;

    private int trainrunId;

    private Time sourceArrival;

    private Time travelTime;

    private Time sourceDeparture;

    private Time targetArrival;

    private Time targetDeparture;

    // TODO: Move this into the converter, this should be read-only value object.
    public void swap() {
        int tmpId = sourceNodeId;
        Time tmpDeparture = sourceDeparture;
        Time tmpArrival = sourceArrival;
        sourceNodeId = targetNodeId;
        sourceDeparture = targetDeparture;
        sourceArrival = targetArrival;
        targetNodeId = tmpId;
        targetDeparture = tmpDeparture;
        targetArrival = tmpArrival;
    }

}

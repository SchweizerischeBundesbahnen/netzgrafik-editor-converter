package ch.sbb.pfi.netzgrafikeditor.converter.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;


@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Node implements Identifiable {

    int id;

    String betriebspunktName;

    String fullName;

    double positionX;

    double positionY;

    int perronkanten;

    int connectionTime;

    List<Port> ports;

    List<Transition> transitions;

    @Builder.Default
    List<Connection> connections = List.of();

    Map<String, TrainrunCategoryHaltezeit> trainrunCategoryHaltezeiten;

    @Builder.Default
    List<Integer> labelIds = List.of();

}

package ch.sbb.pfi.netzgrafikeditor.converter.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {

    List<TrainrunCategory> trainrunCategories;

    List<TrainrunFrequency> trainrunFrequencies;

    List<TrainrunTimeCategory> trainrunTimeCategories;

}

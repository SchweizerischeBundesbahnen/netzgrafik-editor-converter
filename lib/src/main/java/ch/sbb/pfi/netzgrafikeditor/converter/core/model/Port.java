package ch.sbb.pfi.netzgrafikeditor.converter.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class Port implements Identifiable {

    int id;

    int trainrunSectionId;

}

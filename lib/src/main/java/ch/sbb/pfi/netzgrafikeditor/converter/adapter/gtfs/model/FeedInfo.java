package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class FeedInfo {

    public static final LocalDate DEFAULT_START_DATE = LocalDate.of(1970, 1, 1);
    public static final LocalDate DEFAULT_END_DATE = LocalDate.of(2099, 12, 31);

    @Builder.Default
    String feedPublisherName = "Netzgrafik Editor Converter";

    @Builder.Default
    String feedPublisherUrl = "https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-converter";

    @Builder.Default
    String feedLang = "en";

    @Builder.Default
    LocalDate feedStartDate = DEFAULT_START_DATE;

    @Builder.Default
    LocalDate feedEndDate = DEFAULT_END_DATE;

    @Builder.Default
    LocalDateTime feedVersion = LocalDateTime.now();

}

package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

import static ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.FeedInfo.DEFAULT_END_DATE;
import static ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.FeedInfo.DEFAULT_START_DATE;

@Value
@Builder
public class Calendar {

    public static final String DEFAULT_ID = "always";

    @Builder.Default
    String serviceId = DEFAULT_ID;

    @Builder.Default
    Type monday = Type.AVAILABLE;

    @Builder.Default
    Type tuesday = Type.AVAILABLE;

    @Builder.Default
    Type wednesday = Type.AVAILABLE;

    @Builder.Default
    Type thursday = Type.AVAILABLE;

    @Builder.Default
    Type friday = Type.AVAILABLE;

    @Builder.Default
    Type saturday = Type.AVAILABLE;

    @Builder.Default
    Type sunday = Type.AVAILABLE;

    @Builder.Default
    LocalDate startDate = DEFAULT_START_DATE;

    @Builder.Default
    LocalDate endDate = DEFAULT_END_DATE;

    public enum Type {
        AVAILABLE,
        NOT_AVAILABLE
    }
}

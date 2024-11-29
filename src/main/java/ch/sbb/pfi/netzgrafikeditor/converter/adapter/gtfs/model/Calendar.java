package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class Calendar {

    String serviceId;

    Type monday;

    Type tuesday;

    Type wednesday;

    Type thursday;

    Type friday;

    Type saturday;

    Type sunday;

    LocalDate startDate;

    LocalDate endDate;

    public enum Type {
        AVAILABLE,
        NOT_AVAILABLE
    }
}

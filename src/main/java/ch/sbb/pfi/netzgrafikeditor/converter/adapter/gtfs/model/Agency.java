package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Agency {

    String agencyId;

    String agencyName;

    String agencyUrl;

    String agencyTimezone;

}

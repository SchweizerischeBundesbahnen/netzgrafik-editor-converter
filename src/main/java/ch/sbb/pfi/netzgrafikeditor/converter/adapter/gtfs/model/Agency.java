package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Agency {

    public static final String DEFAULT_ID = "nge";

    @Builder.Default
    String agencyId = DEFAULT_ID;

    @Builder.Default
    String agencyName = "Netzgrafik Editor";

    @Builder.Default
    String agencyUrl = "https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-frontend";

    @Builder.Default
    String agencyTimezone = "UTC";

}

package ch.sbb.pfi.netzgrafikeditor.converter;

import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

@Value
@Builder
public class NetworkGraphicConverterConfig {

    private static final int SECONDS_IN_A_DAY = 24 * 60 * 60;

    /**
     * Use the train names as names for the transit lines. Since this field is not mandatory in the NGE, it is per
     * default set to false.
     */
    @Builder.Default
    boolean useTrainNames = false;

    // TODO: Introduce a service day class supporting more than 24 hours, e.g. 02:00 the next day.
    //  Example: https://github.com/naviqore/public-transit-service/blob/d42be281701ed9c34cbdb83dcbf2e3fc3dc31ce1/src/main/java/ch/naviqore/gtfs/schedule/type/ServiceDayTime.java
    /**
     * Time when the operation day starts, default is 05:00, this day.
     */
    @Builder.Default
    LocalTime serviceDayStart = LocalTime.of(5, 0);

    /**
     * Time when the operation day ends, default is 23:00, this day.
     */
    @Builder.Default
    LocalTime serviceDayEnd = LocalTime.of(23, 0);

}

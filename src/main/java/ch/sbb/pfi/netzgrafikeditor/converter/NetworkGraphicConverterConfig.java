package ch.sbb.pfi.netzgrafikeditor.converter;

import lombok.Builder;
import lombok.Value;

import java.time.LocalTime;

@Value
@Builder
public class NetworkGraphicConverterConfig {

    private static final int SECONDS_IN_A_DAY = 24 * 60 * 60;

    /**
     * Use the train names as names for the transit line ids. Since this field is not mandatory in the NGE, it is per
     * default set to false. In the default case the transit line id will be constructed from the category type  and the
     * origin and destination stop of the line.
     */
    @Builder.Default
    boolean useTrainNamesAsIds = false;

    /**
     * If the validation of node names or trainrun names, which are used as IDs, is not successful, the converter fails.
     * If this is set to false, the converter will proceed and only log issues found. Depending on the supply builder
     * choose this can lead to issues, since some target formats are not supporting special characters in the IDs.
     * <p>
     * Note: If the parameter useTrainNamesAsIds is set to false, the trainrun names are not validated, since they are
     * not used in this case.
     */
    @Builder.Default
    boolean failOnValidationIssue = true;

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

package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NetworkGraphicConverterConfig {

    private static final int SECONDS_IN_A_DAY = 24 * 60 * 60;

    /**
     * Use the train names as names for the transit line ids. Since this field is not mandatory in the NGE, it is per
     * default set to false. In the default case the transit line id will be constructed from the category type and the
     * origin and destination stop of the line.
     */
    @Builder.Default
    boolean useTrainNamesAsIds = false;

    /**
     * Strategy for the validation of the network graphic: SKIP_VALIDATION, WARN_ON_ISSUES, FAIL_ON_ISSUES, FIX_ISSUES.
     * The converter uses the node `betriebsPunktName` and the trainrun `name` attributes as IDs to pass to the supply
     * builder. Since these fields are not mandatory in the NGE (as it uses internal integers as IDs), it is important
     * to validate them if they are applicable as IDs. Depending on the chosen supply builder, invalid IDs can lead to
     * downstream issues, as some target formats do not support special characters in the IDs.
     * <p>
     * Note: If the parameter {@link #useTrainNamesAsIds} is set to false, the trainrun names are not considered in
     * validation, since they are not used in this case.
     */
    @Builder.Default
    ValidationStrategy validationStrategy = ValidationStrategy.WARN_ON_ISSUES;

    /**
     * Time when the operation day starts, default is 05:00, this day.
     */
    @Builder.Default
    ServiceDayTime serviceDayStart = ServiceDayTime.of(5, 0, 0);

    /**
     * Time when the operation day ends, default is 23:00, this day.
     */
    @Builder.Default
    ServiceDayTime serviceDayEnd = ServiceDayTime.of(25, 0, 0);

}

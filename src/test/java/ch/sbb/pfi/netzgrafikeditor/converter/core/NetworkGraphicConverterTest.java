package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteDirection;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkGraphicConverterTest {

    @Mock
    private NetworkGraphicConverterConfig config;

    @Mock
    private NetworkGraphicSource source;

    @Mock
    private SupplyBuilder builder;

    @Mock
    private ConverterSink sink;

    @InjectMocks
    private NetworkGraphicConverter converter;

    @BeforeEach
    void setUp() {
        when(config.getServiceDayStart()).thenReturn(LocalTime.MIN);
        when(config.getServiceDayEnd()).thenReturn(LocalTime.MAX);
        when(config.isUseTrainNamesAsIds()).thenReturn(false);
        when(config.getValidationStrategy()).thenReturn(ValidationStrategy.WARN_ON_ISSUES);
        when(builder.addStopFacility(anyString(), anyDouble(), anyDouble())).thenReturn(builder);
        when(builder.addTransitLine(anyString(), anyString(), anyString(), any())).thenReturn(builder);
        when(builder.addRouteStop(anyString(), anyString(), any(), any())).thenReturn(builder);
        when(builder.addDeparture(anyString(), any(RouteDirection.class), any())).thenReturn(builder);
    }

    @ParameterizedTest
    @EnumSource(TestCase.class)
    void run(TestCase testCase) throws IOException {
        when(source.load()).thenReturn(new JsonDeserializer().read(testCase.getPath()));
        converter.run();
        verifyConversionSteps();
    }

    private void verifyConversionSteps() throws IOException {
        verify(source, times(1)).load();
        verify(builder, times(1)).build();
        verify(sink, times(1)).save();
    }

}

package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonDeserializer;
import ch.sbb.pfi.netzgrafikeditor.converter.util.test.TestCase;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Scenario;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkGraphicConverterTest {

    @Mock
    private NetworkGraphicConverterConfig config;

    @Mock
    private NetworkGraphicSource source;

    @Mock
    private SupplyBuilder<Scenario> builder;

    @Mock
    private ConverterSink<Scenario> sink;

    @InjectMocks
    private NetworkGraphicConverter<Scenario> converter;

    @BeforeEach
    void setUp() {
        when(config.getServiceDayStart()).thenReturn(ServiceDayTime.MIN);
        when(config.getServiceDayEnd()).thenReturn(ServiceDayTime.NOON);
        when(config.isUseTrainNamesAsIds()).thenReturn(false);
        when(config.getValidationStrategy()).thenReturn(ValidationStrategy.WARN_ON_ISSUES);
        when(builder.addStopFacility(anyString(), anyDouble(), anyDouble())).thenReturn(builder);
        when(builder.addTransitRoute(anyString(), anyString(), anyString(), any())).thenReturn(builder);
        when(builder.addRouteStop(anyString(), anyString(), any(), any())).thenReturn(builder);
        when(builder.addDeparture(anyString(), any())).thenReturn(builder);
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
        verify(sink, times(1)).save(any());
    }

}

package ch.sbb.pfi.netzgrafikeditor.converter;

import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonDeserializer;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.RouteDirection;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.SupplyBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetzgrafikConverterTest {

    @Mock
    private NetworkGraphicSource source;

    @Mock
    private SupplyBuilder builder;

    @Mock
    private ConverterSink sink;

    @InjectMocks
    private NetzgrafikConverter converter;

    @BeforeEach
    void setUp() {
        when(builder.addStopFacility(anyString())).thenReturn(builder);
        when(builder.addTransitLine(anyString(), anyString(), anyString(), any())).thenReturn(builder);
        when(builder.addRouteStop(anyString(), anyString(), any(), any())).thenReturn(builder);
        when(builder.addDeparture(anyString(), any(RouteDirection.class), any())).thenReturn(builder);
    }

    @Test
    void convert_simple() throws IOException {
        when(source.load()).thenReturn(new JsonDeserializer().read(TestData.SIMPLE.getPath()));

        converter.run();

        verifyConversionSteps();
    }

    @Test
    void convert_cycle() throws IOException {
        when(source.load()).thenReturn(new JsonDeserializer().read(TestData.CYCLE.getPath()));

        converter.run();

        verifyConversionSteps();
    }

    @Test
    void convert_conflictingTimes() throws IOException {
        when(source.load()).thenReturn(new JsonDeserializer().read(TestData.CONFLICTING_TIMES.getPath()));
        when(builder.addRoutePass(anyString(), anyString())).thenReturn(builder);

        converter.run();

        verifyConversionSteps();
    }

    private void verifyConversionSteps() throws IOException {
        verify(source, times(1)).load();
        verify(builder, times(1)).build();
        verify(sink, times(1)).save();
    }

}

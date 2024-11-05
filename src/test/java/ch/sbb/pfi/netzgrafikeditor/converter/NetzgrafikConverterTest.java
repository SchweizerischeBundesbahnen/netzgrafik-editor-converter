package ch.sbb.pfi.netzgrafikeditor.converter;

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
    private SupplyBuilder builder;

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
        converter.read(TestData.SIMPLE.getPath());

        verify(builder, times(1)).build();
    }

    @Test
    void convert_cycle() throws IOException {
        converter.read(TestData.CYCLE.getPath());

        verify(builder, times(1)).build();
    }

    @Test
    void convert_conflictingTimes() throws IOException {
        when(builder.addRoutePass(anyString(), anyString())).thenReturn(builder);

        converter.read(TestData.CONFLICTING_TIMES.getPath());

        verify(builder, times(1)).build();
    }

}

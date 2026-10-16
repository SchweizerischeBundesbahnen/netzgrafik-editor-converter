package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonDeserializer;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestCase;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestScenario;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Scenario;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;

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
        when(config.isUseTrainNamesAsIds()).thenReturn(false);
        when(config.getValidationStrategy()).thenReturn(ValidationStrategy.WARN_ON_ISSUES);
        when(builder.addStopFacility(anyString(), anyString(), anyDouble(), anyDouble())).thenReturn(builder);
        when(builder.addTransitRoute(anyString(), anyString(), anyString(), any())).thenReturn(builder);
        when(builder.addRouteStop(anyString(), anyString(), any(), any())).thenReturn(builder);
        when(builder.addDeparture(anyString(), any())).thenReturn(builder);
    }

    @ParameterizedTest
    @EnumSource(TestCase.class)
    void run(TestCase testCase) throws IOException {
        configureServiceDay(ServiceDayTime.MIN, ServiceDayTime.NOON, ServiceDayBoundary.FULL);
        when(source.load()).thenReturn(new JsonDeserializer().read(testCase.getPath()));
        converter.run();
        verifyConversionSteps();
    }

    @Test
    void run_asymmetricTravelTime() throws IOException {
        configureServiceDay(ServiceDayTime.MIN, ServiceDayTime.NOON, ServiceDayBoundary.FULL);
        when(source.load()).thenReturn(new JsonDeserializer().read(TestCase.SIMPLE_ASYMMETRIC.getPath()));
        converter.run();

        // section A-B takes 15 minutes forward, but 18 minutes backward
        verify(builder).addRouteStop(eq("IC_A_D_FORWARD"), eq("B"), eq(Duration.ofMinutes(15)), any());
        verify(builder).addRouteStop(eq("IC_A_D_REVERSE"), eq("A"), eq(Duration.ofMinutes(18)), any());
    }

    @Test
    void run_clipServiceDayStart() throws IOException {
        configureServiceDay(ServiceDayTime.of(6, 0, 0), ServiceDayTime.of(25, 0, 0), ServiceDayBoundary.CLIP);
        when(source.load()).thenReturn(new JsonDeserializer().read(TestCase.CYCLE.getPath()));
        converter.run();

        // trips running at 06:00 start at their first stop departing after 06:00
        verify(builder).addDeparture("S_A_A_FORWARD_C_A_2_8", ServiceDayTime.of(6, 6, 0));
        verify(builder).addDeparture("S_A_A_FORWARD_E_A_4_8", ServiceDayTime.of(6, 7, 0));
        verify(builder).addDeparture("S_A_A_FORWARD_F_A_5_8", ServiceDayTime.of(6, 0, 0));
        verify(builder).addDeparture("S_A_A_FORWARD_H_A_7_8", ServiceDayTime.of(6, 1, 0));
        verify(builder).addDeparture("S_A_A_FORWARD", ServiceDayTime.of(6, 5, 0));
        verify(builder).addTransitRoute("S_A_A_FORWARD_H_A_7_8", "S_A_A", "H", Duration.ofMinutes(1));
        verify(builder).addRouteStop("S_A_A_FORWARD_H_A_7_8", "A", Duration.ofMinutes(3), Duration.ofMinutes(1));
        verify(builder, never()).addDeparture(anyString(),
                argThat(time -> time.compareTo(ServiceDayTime.of(6, 0, 0)) < 0));
    }

    @Test
    void run_clipServiceDayEnd() throws IOException {
        configureServiceDay(ServiceDayTime.of(5, 0, 0), ServiceDayTime.of(7, 30, 0), ServiceDayBoundary.CLIP);
        when(source.load()).thenReturn(new JsonDeserializer().read(TestCase.SIMPLE.getPath()));
        converter.run();

        // the trip at 07:05 arrives in B (07:20) before, but in C (07:42) after the end of the service day
        verify(builder).addDeparture("IC_A_D_FORWARD", ServiceDayTime.of(6, 5, 0));
        verify(builder).addDeparture("IC_A_D_FORWARD_A_B_0_1", ServiceDayTime.of(7, 5, 0));
        verify(builder).addRouteStop("IC_A_D_FORWARD_A_B_0_1", "B", Duration.ofMinutes(15), Duration.ofMinutes(2));
        verify(builder, never()).addDeparture("IC_A_D_FORWARD", ServiceDayTime.of(7, 5, 0));
    }

    @Test
    void run_depotServiceDayStart() throws IOException {
        configureServiceDay(ServiceDayTime.of(6, 0, 0), ServiceDayTime.of(25, 0, 0), ServiceDayBoundary.DEPOT);
        when(config.getDepotLabel()).thenReturn("Depot");
        when(source.load()).thenReturn(new JsonDeserializer().read(TestCase.CYCLE.getPath()));
        converter.run();

        // trips clipped to C, E, F and H start at the first depot D after them, if any
        verify(builder).addTransitRoute(eq("S_A_A_FORWARD_D_A_3_8"), eq("S_A_A"), eq("D"), any());
        verify(builder).addDeparture("S_A_A_FORWARD_D_A_3_8", ServiceDayTime.of(6, 14, 0));
        verify(builder).addDeparture("S_A_A_FORWARD", ServiceDayTime.of(6, 5, 0));
        verify(builder, never()).addDeparture(anyString(),
                argThat(time -> time.compareTo(ServiceDayTime.of(6, 0, 0)) < 0));
    }

    @Test
    void run_depotServiceDayEnd() throws IOException {
        configureServiceDay(ServiceDayTime.of(5, 0, 0), ServiceDayTime.of(7, 50, 0), ServiceDayBoundary.DEPOT);
        when(config.getDepotLabel()).thenReturn("Depot");
        when(source.load()).thenReturn(new JsonDeserializer().read(TestCase.SIMPLE.getPath()));
        converter.run();

        // the trip clipped to C ends at the depot B before it
        verify(builder).addDeparture("IC_A_D_FORWARD_A_B_0_1", ServiceDayTime.of(7, 5, 0));
        verify(builder, never()).addTransitRoute(eq("IC_A_D_FORWARD_A_C_0_2"), anyString(), anyString(), any());
    }

    @Test
    void run_depotWithoutDepots() throws IOException {
        configureServiceDay(ServiceDayTime.of(5, 0, 0), ServiceDayTime.of(7, 30, 0), ServiceDayBoundary.DEPOT);
        when(config.getDepotLabel()).thenReturn("Unknown");
        when(source.load()).thenReturn(new JsonDeserializer().read(TestCase.SIMPLE.getPath()));
        converter.run();

        // no node has the label, so the termini are the only depots and trips not reaching a terminus are dropped
        verify(builder).addDeparture("IC_A_D_FORWARD", ServiceDayTime.of(6, 5, 0));
        verify(builder, never()).addDeparture(anyString(), eq(ServiceDayTime.of(7, 5, 0)));
    }

    @Test
    void run_departuresIndependentOfServiceDayStart() throws IOException {
        configureServiceDay(ServiceDayTime.of(6, 30, 0), ServiceDayTime.of(9, 0, 0), ServiceDayBoundary.FULL);
        when(source.load()).thenReturn(new JsonDeserializer().read(TestCase.SIMPLE.getPath()));
        converter.run();

        // the trainrun departs at minute 05 of every hour, independent of the service day start
        verify(builder).addDeparture("IC_A_D_FORWARD", ServiceDayTime.of(7, 5, 0));
        verify(builder).addDeparture("IC_A_D_FORWARD", ServiceDayTime.of(8, 5, 0));
        verify(builder, never()).addDeparture("IC_A_D_FORWARD", ServiceDayTime.of(6, 35, 0));
    }

    @Test
    void run_twoHourlyDeparturesFollowConsecutiveTime() throws IOException {
        configureServiceDay(ServiceDayTime.of(4, 0, 0), ServiceDayTime.of(25, 0, 0), ServiceDayBoundary.FULL);
        when(source.load()).thenReturn(
                new JsonDeserializer().read(TestScenario.REALISTIC_SCENARIO.getNetworkGraphicFilePath()));
        converter.run();

        // the NGE places the even-hour trainrun at 03:02 in LG, so it departs there in the odd hours
        verify(builder).addDeparture("IC_LG_BS_FORWARD", ServiceDayTime.of(5, 2, 0));
        verify(builder, never()).addDeparture("IC_LG_BS_FORWARD", ServiceDayTime.of(4, 2, 0));
    }

    private void configureServiceDay(ServiceDayTime start, ServiceDayTime end, ServiceDayBoundary boundary) {
        when(config.getServiceDayStart()).thenReturn(start);
        when(config.getServiceDayEnd()).thenReturn(end);
        when(config.getServiceDayBoundary()).thenReturn(boundary);
    }

    private void verifyConversionSteps() throws IOException {
        verify(source, times(1)).load();
        verify(builder, times(1)).build();
        verify(sink, times(1)).save(any());
    }

}

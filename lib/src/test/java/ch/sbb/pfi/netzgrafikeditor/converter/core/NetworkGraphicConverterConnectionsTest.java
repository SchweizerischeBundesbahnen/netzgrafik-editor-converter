package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopAccess;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonFileReader;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestFile;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Converts the connections drawn in a network graphic.
 * <p>
 * In {@code connections.json}, all connections are at node B: from X (every 30 min) to Y (every 60 min) with 4 min to
 * change, from the freight trainrun G1 to the freight trainrun G2 passing B without stopping, in no time, and from X to
 * G2. The freight trainruns are in category G. In {@code connection-times.json}, P arrives at B 1 min after Q departs
 * (missed) and 2 min before R departs (below the connection time of 3 min). In {@code connection-one-way.json}, the
 * freight trainruns F and H run one way, from A to B and from B to C, and the connection at B is drawn from H to F; in
 * {@code connection-against-direction.json}, F and K both end at B.
 */
@ExtendWith(MockitoExtension.class)
class NetworkGraphicConverterConnectionsTest {

    private static final TestFile CONNECTIONS = new TestFile("ng/connections", "connections.json");
    private static final TestFile CONNECTION_TIMES = new TestFile("ng/connections", "connection-times.json");
    private static final TestFile CONNECTION_ONE_WAY = new TestFile("ng/connections", "connection-one-way.json");
    private static final TestFile CONNECTION_AGAINST_DIRECTION = new TestFile("ng/connections",
            "connection-against-direction.json");

    @Mock
    private SupplyBuilder<GtfsSchedule> builder;

    @Mock
    private ConverterSink<GtfsSchedule> sink;

    private static NetworkGraphicConverterConfig.NetworkGraphicConverterConfigBuilder config(ValidationStrategy strategy) {
        return NetworkGraphicConverterConfig.builder()
                .validationStrategy(strategy)
                .serviceDayStart(ServiceDayTime.of(6, 0, 0))
                .serviceDayEnd(ServiceDayTime.of(8, 0, 0));
    }

    private static NetworkGraphicConverterConfig network(ValidationStrategy strategy) {
        return config(strategy).nonPassengerCategories(Set.of("G")).nonPassengerNetwork(true).build();
    }

    private void convert(TestFile networkGraphic, NetworkGraphicConverterConfig config) throws IOException {
        new NetworkGraphicConverter<>(config, new JsonFileReader(networkGraphic.getPath()), builder, sink).run();
    }

    @Nested
    class PassengerService {

        @Test
        void connectionsLinkTheTripsDrawn() throws IOException {
            convert(CONNECTIONS, config(ValidationStrategy.WARN_ON_ISSUES).build());

            // X arrives at B at 06:10 and Y departs at 06:14; the X arriving at 06:40 has no Y to connect to
            verify(builder).addConnection("B", "IC_A_B_FORWARD", ServiceDayTime.of(6, 5, 0), "IC_B_C_FORWARD",
                    ServiceDayTime.of(6, 14, 0));
            verify(builder).addConnection("B", "IC_A_B_FORWARD", ServiceDayTime.of(7, 5, 0), "IC_B_C_FORWARD",
                    ServiceDayTime.of(7, 14, 0));
            verify(builder, never()).addConnection(eq("B"), eq("IC_A_B_FORWARD"), eq(ServiceDayTime.of(6, 35, 0)),
                    anyString(), any());

            // both are round trips, so the connection also applies the other way round: Y arrives at 06:46, X departs
            verify(builder).addConnection("B", "IC_B_C_REVERSE", ServiceDayTime.of(6, 40, 0), "IC_A_B_REVERSE",
                    ServiceDayTime.of(6, 50, 0));
            verify(builder).addConnection("B", "IC_B_C_REVERSE", ServiceDayTime.of(7, 40, 0), "IC_A_B_REVERSE",
                    ServiceDayTime.of(7, 50, 0));

            // G2 passes B without stopping, so nobody changes to or from it; no transfer is forbidden
            verify(builder, times(4)).addConnection(anyString(), anyString(), any(), anyString(), any());
            verify(builder, never()).forbidTransfers(anyString());
        }

        @Test
        void connectionAtAPassFailsOnIssues() {
            NetworkGraphicConverterConfig config = config(ValidationStrategy.FAIL_ON_ISSUES).build();

            assertThrows(IllegalStateException.class, () -> convert(CONNECTIONS, config));
        }

        @Test
        void missedConnectionIsLeftOut() throws IOException {
            convert(CONNECTION_TIMES, config(ValidationStrategy.WARN_ON_ISSUES).build());

            verify(builder, never()).addConnection(anyString(), anyString(), any(), eq("IC_B_C_FORWARD"), any());
            verify(builder, never()).addConnection(anyString(), eq("IC_B_C_REVERSE"), any(), anyString(), any());
        }

        @Test
        void connectionBelowTheConnectionTimeIsKept() throws IOException {
            convert(CONNECTION_TIMES, config(ValidationStrategy.WARN_ON_ISSUES).build());

            // the departing train waits, so the connection holds although it is shorter than the node's 3 min
            verify(builder).addConnection("B", "IC_A_B_FORWARD", ServiceDayTime.of(6, 0, 0), "IC_B_D_FORWARD",
                    ServiceDayTime.of(6, 12, 0));
            verify(builder).addConnection("B", "IC_B_D_REVERSE", ServiceDayTime.of(6, 43, 0), "IC_A_B_REVERSE",
                    ServiceDayTime.of(6, 50, 0));
        }

        @Test
        void connectionIssuesAreHandledByTheStrategy() {
            assertThrows(IllegalStateException.class,
                    () -> convert(CONNECTION_TIMES, config(ValidationStrategy.FAIL_ON_ISSUES).build()));
            assertDoesNotThrow(() -> convert(CONNECTION_TIMES, config(ValidationStrategy.SKIP_VALIDATION).build()));
            assertDoesNotThrow(() -> convert(CONNECTION_TIMES, config(ValidationStrategy.REPLACE_WHITESPACE).build()));
        }

    }

    @Nested
    class NonPassengerService {

        /**
         * The freight trainruns run, but nobody boards them, so their connections are left out without an issue (the
         * conversion fails on issues otherwise), and the passenger connections are kept.
         */
        @Test
        void trainrunsOfANonPassengerCategoryAreNotBoardable() throws IOException {
            convert(CONNECTIONS, config(ValidationStrategy.FAIL_ON_ISSUES).nonPassengerCategories(Set.of("G")).build());

            verify(builder).addTransitRoute(eq("G_D_B_FORWARD"), eq("G_D_B"), eq("D"), any(), eq(StopAccess.NONE));
            verify(builder).addRouteStop(eq("G_D_B_FORWARD"), eq("B"), any(), any(), eq(StopAccess.NONE));
            verify(builder, atLeastOnce()).addRouteStop(eq("IC_A_B_FORWARD"), eq("B"), any(), any(),
                    eq(StopAccess.BOARDING_AND_ALIGHTING));
            verify(builder, times(4)).addConnection(anyString(), anyString(), any(), anyString(), any());
        }

    }

    @Nested
    class NonPassengerNetwork {

        @Test
        void trainrunsWithPassengerServiceAreLeftOut() throws IOException {
            convert(CONNECTIONS, network(ValidationStrategy.WARN_ON_ISSUES));

            verify(builder).addTransitLine("G_D_B", "G");
            verify(builder).addTransitLine("G_E_C", "G");
            verify(builder, never()).addTransitLine(eq("IC_A_B"), anyString());
            verify(builder, never()).addTransitLine(eq("IC_B_C"), anyString());
        }

        @Test
        void trainsChangeInNoTime() throws IOException {
            convert(CONNECTIONS, network(ValidationStrategy.WARN_ON_ISSUES));

            // the trainruns start and end without dwell time; G1 arrives at B as G2 passes it
            verify(builder).addTransitRoute("G_D_B_FORWARD", "G_D_B", "D", Duration.ZERO,
                    StopAccess.BOARDING_AND_ALIGHTING);
            verify(builder).addRouteStop("G_D_B_FORWARD", "B", Duration.ofMinutes(5), Duration.ZERO,
                    StopAccess.BOARDING_AND_ALIGHTING);
            verify(builder).addConnection("B", "G_D_B_FORWARD", ServiceDayTime.of(6, 15, 0), "G_E_C_FORWARD",
                    ServiceDayTime.of(6, 15, 0));
            verify(builder).addConnection("B", "G_E_C_REVERSE", ServiceDayTime.of(6, 34, 0), "G_D_B_REVERSE",
                    ServiceDayTime.of(6, 40, 0));
        }

        @Test
        void passWithAConnectionBecomesATechnicalStop() throws IOException {
            convert(CONNECTIONS, network(ValidationStrategy.WARN_ON_ISSUES));

            // G2 stops at B in no time: boarding only where it departs after the change, alighting only where it
            // arrives for the change
            verify(builder).addRouteStop("G_E_C_FORWARD", "B", Duration.ofMinutes(5), Duration.ZERO,
                    StopAccess.BOARDING_ONLY);
            verify(builder).addRouteStop("G_E_C_REVERSE", "B", Duration.ofMinutes(6), Duration.ZERO,
                    StopAccess.ALIGHTING_ONLY);
            verify(builder, never()).addRoutePass(anyString(), eq("B"));
        }

        @Test
        void transfersAreForbiddenAtEveryStopServed() throws IOException {
            convert(CONNECTIONS, network(ValidationStrategy.WARN_ON_ISSUES));

            for (String stop : Set.of("B", "C", "D", "E")) {
                verify(builder).forbidTransfers(stop);
            }
            verify(builder, never()).forbidTransfers("A");
        }

        @Test
        void connectionToATrainrunWithPassengerServiceIsAnIssue() {
            assertThrows(IllegalStateException.class,
                    () -> convert(CONNECTIONS, network(ValidationStrategy.FAIL_ON_ISSUES)));
        }

        @Test
        void requiresTrainrunsWithoutPassengerService() {
            NetworkGraphicConverterConfig config = config(ValidationStrategy.WARN_ON_ISSUES).nonPassengerNetwork(true)
                    .build();

            assertThrows(IllegalStateException.class, () -> convert(CONNECTIONS, config));
        }

    }

    @Nested
    class OneWayTrainruns {

        /**
         * The order of the ports is the order the connection was drawn in: F arrives at B as H departs, in no time.
         */
        @Test
        void connectionAppliesTheWayTheTrainrunsRun() throws IOException {
            convert(CONNECTION_ONE_WAY, network(ValidationStrategy.FAIL_ON_ISSUES));

            verify(builder).addConnection("B", "G_A_B_FORWARD", ServiceDayTime.of(6, 5, 0), "G_B_C_FORWARD",
                    ServiceDayTime.of(6, 10, 0));
            verify(builder).addConnection("B", "G_A_B_FORWARD", ServiceDayTime.of(7, 5, 0), "G_B_C_FORWARD",
                    ServiceDayTime.of(7, 10, 0));
            verify(builder, times(2)).addConnection(anyString(), anyString(), any(), anyString(), any());
        }

        @Test
        void connectionAgainstTheDirectionOfBothTrainrunsIsAnIssue() {
            assertThrows(IllegalStateException.class,
                    () -> convert(CONNECTION_AGAINST_DIRECTION, network(ValidationStrategy.FAIL_ON_ISSUES)));
        }

        @Test
        void connectionAgainstTheDirectionOfBothTrainrunsIsLeftOut() throws IOException {
            convert(CONNECTION_AGAINST_DIRECTION, network(ValidationStrategy.WARN_ON_ISSUES));

            verify(builder, never()).addConnection(anyString(), anyString(), any(), anyString(), any());
        }

    }

}

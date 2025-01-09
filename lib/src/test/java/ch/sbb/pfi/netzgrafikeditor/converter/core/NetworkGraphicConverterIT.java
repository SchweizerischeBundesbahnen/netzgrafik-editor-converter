package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.GtfsSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.StopTime;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim.MatsimSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoVehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.io.gtfs.GtfsScheduleWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.matsim.TransitScheduleXmlWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonFileReader;
import ch.sbb.pfi.netzgrafikeditor.converter.test.OutputDir;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestCase;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestDirectoryExtension;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TestDirectoryExtension.class)
public class NetworkGraphicConverterIT {

    public static final String CASE_SEPARATOR = ".";
    public static final String DELIMITER = "-";

    private Path outputDir;

    @BeforeEach
    void setUp(@OutputDir Path outputDir) {
        this.outputDir = outputDir;
    }

    @Nested
    class MatsimTransitSchedule {
        private Scenario scenario;
        private NetworkGraphicConverter<Scenario> converter;

        @ParameterizedTest
        @EnumSource(TestScenario.class)
        void run(TestScenario testScenario) throws IOException {
            configure(testScenario.getNetworkGraphicFilePath(), testScenario.name());
            converter.run();
            assertNotNull(scenario);
        }

        @ParameterizedTest
        @EnumSource(TestCase.class)
        void run(TestCase testCase) throws IOException {
            configure(testCase.getPath(), testCase.name());
            converter.run();
            validate(testCase);
        }

        private void validate(TestCase testCase) {

            // check scenario
            assertNotNull(scenario);
            assertEquals(1, scenario.getTransitSchedule().getTransitLines().size());
            assertEquals(1, scenario.getTransitVehicles().getVehicleTypes().size());
            assertFalse(scenario.getTransitVehicles().getVehicles().isEmpty());

            // check transit line
            TransitLine transitLine = scenario.getTransitSchedule()
                    .getTransitLines()
                    .get(Id.create(testCase.name(), TransitLine.class));
            assertNotNull(transitLine);
            assertEquals(2, transitLine.getRoutes().size());

            // check FORWARD transit route
            TransitRoute transitRoute = transitLine.getRoutes()
                    .get(Id.create(testCase.name() + "_" + RouteDirection.FORWARD, TransitRoute.class));
            assertNotNull(transitRoute);

            // expected stop sequence
            String expectedStopSequenceForward = String.join(DELIMITER, testCase.getStopSequence());

            // actual stop sequence for the transitRoute
            String actualStopSequence = transitRoute.getStops()
                    .stream()
                    .map(s -> s.getStopFacility().getId().toString())
                    .collect(Collectors.joining(DELIMITER));

            // ensure the actual stop sequence matches either the actual sequence
            assertEquals(expectedStopSequenceForward, actualStopSequence);
        }

        private void configure(Path path, String prefix) {
            NetworkGraphicConverterConfig config = NetworkGraphicConverterConfig.builder()
                    .useTrainNamesAsIds(true)
                    .build();

            NetworkGraphicSource source = new JsonFileReader(path);
            SupplyBuilder<Scenario> builder = new MatsimSupplyBuilder(new NoInfrastructureRepository(),
                    new NoVehicleCircuitsPlanner(new NoRollingStockRepository()));

            // store scenario and write schedule
            ConverterSink<Scenario> sink = result -> {
                scenario = result;
                new TransitScheduleXmlWriter(outputDir, prefix.toLowerCase() + CASE_SEPARATOR).save(result);
            };

            converter = new NetworkGraphicConverter<>(config, source, builder, sink);
        }
    }

    @Nested
    class GtfsStaticSchedule {
        private GtfsSchedule schedule;
        private NetworkGraphicConverter<GtfsSchedule> converter;

        private static void validateCurrentSequence(TestCase testCase, String actualSequence, boolean reversed) {
            List<String> stopSequence = testCase.getStopSequence();

            if (reversed) {
                List<String> reversedSequence = new ArrayList<>(stopSequence);
                Collections.reverse(reversedSequence);
                String expectedSequence = String.join(DELIMITER, reversedSequence);

                assertEquals(expectedSequence, actualSequence);
            } else {
                String expectedSequence = String.join(DELIMITER, stopSequence);
                assertEquals(expectedSequence, actualSequence);
            }
        }


        @ParameterizedTest
        @EnumSource(TestScenario.class)
        void run(TestScenario testScenario) throws IOException {
            configure(testScenario.getNetworkGraphicFilePath());
            converter.run();
            assertNotNull(schedule);
        }

        @ParameterizedTest
        @EnumSource(TestCase.class)
        void run(TestCase testCase) throws IOException {
            configure(testCase.getPath());
            converter.run();
            validate(testCase);
        }

        private void validate(TestCase testCase) {
            assertNotNull(schedule);
            assertEquals(1, schedule.getAgencies().size());
            assertEquals(1, schedule.getCalendars().size());
            assertEquals(1, schedule.getRoutes().size());
            assertFalse(schedule.getTrips().isEmpty());

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            boolean reversed = false;
            for (StopTime stopTime : schedule.getStopTimes()) {

                // end of current sequence
                if (stopTime.getStopSequence() == 1 && !first) {
                    validateCurrentSequence(testCase, sb.toString(), reversed);

                    // reset
                    first = true;
                    sb.setLength(0);
                }

                // store direction
                reversed = stopTime.getTripId().contains(RouteDirection.REVERSE.name());

                // add new stop id
                if (!sb.isEmpty()) {
                    sb.append(DELIMITER);
                    first = false;
                }
                sb.append(stopTime.getStopId());
            }

            // validate the last sequence after the loop
            validateCurrentSequence(testCase, sb.toString(), reversed);

        }

        private void configure(Path path) {
            NetworkGraphicConverterConfig config = NetworkGraphicConverterConfig.builder()
                    .useTrainNamesAsIds(true)
                    .build();

            NetworkGraphicSource source = new JsonFileReader(path);
            SupplyBuilder<GtfsSchedule> builder = new GtfsSupplyBuilder(new NoInfrastructureRepository(),
                    new NoVehicleCircuitsPlanner(new NoRollingStockRepository()));

            // store and write schedule
            ConverterSink<GtfsSchedule> sink = result -> {
                schedule = result;
                new GtfsScheduleWriter(outputDir, false).save(result);
            };

            converter = new NetworkGraphicConverter<>(config, source, builder, sink);
        }
    }

}

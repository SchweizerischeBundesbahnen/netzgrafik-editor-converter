package ch.sbb.pfi.netzgrafikeditor.converter.core;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoVehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.io.matsim.TransitScheduleXmlWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonFileReader;
import ch.sbb.pfi.netzgrafikeditor.converter.matsim.MatsimSupplyBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkGraphicConverterIT {

    public static final String OUTPUT_ROOT = "integration-test/output/";
    public static final Path OUTPUT_PATH = Path.of(
            OUTPUT_ROOT + NetworkGraphicConverterIT.class.getCanonicalName().replace(".", "/"));
    public static final String CASE_SEPARATOR = ".";

    private Scenario scenario;
    private NetworkGraphicConverter converter;

    @BeforeEach
    void setUp() {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
    }

    @ParameterizedTest
    @EnumSource(TestScenario.class)
    void run(TestScenario testScenario) throws IOException {
        configure(testScenario.getPath(), testScenario.name());
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
        String expectedStopSequenceForward = String.join("-", testCase.getStopSequence());

        // actual stop sequence for the transitRoute
        String actualStopSequence = transitRoute.getStops()
                .stream()
                .map(s -> s.getStopFacility().getId().toString())
                .collect(Collectors.joining("-"));

        // ensure the actual stop sequence matches either the actual sequence
        assertEquals(expectedStopSequenceForward, actualStopSequence);
    }

    private void configure(Path path, String prefix) {
        NetworkGraphicConverterConfig config = NetworkGraphicConverterConfig.builder().useTrainNamesAsIds(true).build();

        NetworkGraphicSource source = new JsonFileReader(path);
        SupplyBuilder builder = new MatsimSupplyBuilder(scenario, new NoInfrastructureRepository(),
                new NoVehicleCircuitsPlanner(new NoRollingStockRepository()));
        ConverterSink sink = new TransitScheduleXmlWriter(scenario, OUTPUT_PATH.resolve(prefix.toLowerCase()),
                prefix.toLowerCase() + CASE_SEPARATOR);

        converter = new NetworkGraphicConverter(config, source, builder, sink);
    }
}

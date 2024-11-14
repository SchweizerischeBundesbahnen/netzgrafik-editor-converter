package ch.sbb.pfi.netzgrafikeditor.converter;

import ch.sbb.pfi.netzgrafikeditor.converter.io.matsim.TransitScheduleXmlWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonFileReader;
import ch.sbb.pfi.netzgrafikeditor.converter.matsim.MatsimSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.fallback.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.fallback.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.fallback.NoVehicleCircuitsPlanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NetworkGraphicConverterIT {

    public static final String OUTPUT_ROOT = "integration-test/output/";
    public static final Path OUTPUT_PATH = Path.of(
            OUTPUT_ROOT + NetworkGraphicConverterIT.class.getCanonicalName().replace(".", "/"));

    private Scenario scenario;
    private NetworkGraphicConverter converter;

    @BeforeEach
    void setUp() {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
    }

    @ParameterizedTest
    @EnumSource(TestCase.class)
    void convert(TestCase testCase) throws IOException {
        configureConverter(testCase);
        converter.run();
        assertNotNull(scenario);
    }

    private void configureConverter(TestCase testCase) throws IOException {
        NetworkGraphicSource source = new JsonFileReader(testCase.getPath());
        SupplyBuilder builder = new MatsimSupplyBuilder(scenario, new NoInfrastructureRepository(source.load()),
                new NoRollingStockRepository(), new NoVehicleCircuitsPlanner());
        ConverterSink sink = new TransitScheduleXmlWriter(scenario, OUTPUT_PATH, testCase.name().toLowerCase() + ".");

        converter = new NetworkGraphicConverter(NetworkGraphicConverterConfig.builder().useTrainNames(true).build(),
                source, builder, sink);
    }
}

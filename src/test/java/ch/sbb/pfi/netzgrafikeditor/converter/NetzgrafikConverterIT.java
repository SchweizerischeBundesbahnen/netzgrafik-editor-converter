package ch.sbb.pfi.netzgrafikeditor.converter;

import ch.sbb.pfi.netzgrafikeditor.converter.io.matsim.TransitScheduleXmlWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonFileReader;
import ch.sbb.pfi.netzgrafikeditor.converter.matsim.MatsimSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.impl.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.impl.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.impl.NoVehicleCircuitsPlanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NetzgrafikConverterIT {

    public static final String OUTPUT_ROOT = "integration-test/output/";
    public static final Path OUTPUT_PATH = Path.of(
            OUTPUT_ROOT + NetzgrafikConverterIT.class.getCanonicalName().replace(".", "/"));

    private Scenario scenario;
    private NetzgrafikConverter converter;

    @BeforeEach
    void setUp() {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
    }

    @Test
    void convert_simple() throws IOException {
        configureConverter(TestData.SIMPLE);
        converter.run();
        assertNotNull(scenario);
    }

    @Test
    void convert_cycle() throws IOException {
        configureConverter(TestData.CYCLE);
        converter.run();
        assertNotNull(scenario);
    }

    @Test
    void convert_conflictingTimes() throws IOException {
        configureConverter(TestData.CONFLICTING_TIMES);
        converter.run();
        assertNotNull(scenario);
    }

    private void configureConverter(TestData testData) throws IOException {
        NetworkGraphicSource source = new JsonFileReader(testData.getPath());
        SupplyBuilder builder = new MatsimSupplyBuilder(scenario, new NoInfrastructureRepository(source.load()),
                new NoRollingStockRepository(), new NoVehicleCircuitsPlanner());
        ConverterSink sink = new TransitScheduleXmlWriter(scenario, OUTPUT_PATH, testData.name().toLowerCase() + ".");

        converter = new NetzgrafikConverter(source, builder, sink);
    }
}

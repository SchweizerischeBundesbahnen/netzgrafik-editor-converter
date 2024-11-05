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
import org.junit.jupiter.api.io.TempDir;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.nio.file.Path;

public class NetzgrafikConverterIT {

    private Scenario scenario;
    private NetworkGraphicSource source;
    private SupplyBuilder builder;
    private ConverterSink sink;
    private NetzgrafikConverter converter;

    @TempDir
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        source = new JsonFileReader(TestData.SIMPLE.getPath());
        builder = new MatsimSupplyBuilder(scenario, new NoInfrastructureRepository(source.load()),
                new NoRollingStockRepository(), new NoVehicleCircuitsPlanner());
        sink = new TransitScheduleXmlWriter(scenario, tempDir, "test.");
        converter = new NetzgrafikConverter(source, builder, sink);
    }

    @Test
    void convert_simple() throws IOException {
        converter.run();
    }


}

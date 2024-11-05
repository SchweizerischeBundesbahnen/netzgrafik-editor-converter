package ch.sbb.pfi.netzgrafikeditor;

import ch.sbb.pfi.netzgrafikeditor.converter.ConverterSink;
import ch.sbb.pfi.netzgrafikeditor.converter.NetworkGraphicSource;
import ch.sbb.pfi.netzgrafikeditor.converter.NetzgrafikConverter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.matsim.TransitScheduleXmlWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonFileReader;
import ch.sbb.pfi.netzgrafikeditor.converter.matsim.MatsimSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.impl.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.impl.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.impl.NoVehicleCircuitsPlanner;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class Application implements CommandLineRunner {

    private Path outputPath;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 2) {
            System.err.println("Please provide the path to the netzgrafik JSON file and the output directory.");
            System.exit(1);
        }

        String jsonFilePath = args[0];
        String outputDirPath = args[1];
        outputPath = Paths.get(outputDirPath);

        convertJsonToMatsimSchedule(Paths.get(jsonFilePath));
    }

    private void convertJsonToMatsimSchedule(Path jsonFilePath) {
        try {
            Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            NetworkGraphicSource source = new JsonFileReader(jsonFilePath);
            SupplyBuilder builder = new MatsimSupplyBuilder(scenario, new NoInfrastructureRepository(source.load()),
                    new NoRollingStockRepository(), new NoVehicleCircuitsPlanner());

            String baseFilename = jsonFilePath.getFileName().toString();
            String filenameWithoutExtension = jsonFilePath.getFileName()
                    .toString()
                    .substring(0, baseFilename.lastIndexOf('.'));
            ConverterSink sink = new TransitScheduleXmlWriter(scenario, outputPath, filenameWithoutExtension + ".");

            NetzgrafikConverter converter = new NetzgrafikConverter(source, builder, sink);
            converter.run();

            System.out.println("MATSim schedule has been written to: " + outputPath);
            System.exit(0);
        } catch (IOException e) {
            System.err.println("Error during conversion: " + e.getMessage());
            System.exit(1);
        }
    }
}

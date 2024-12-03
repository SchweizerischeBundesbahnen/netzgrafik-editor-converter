package ch.sbb.pfi.netzgrafikeditor.converter.app;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.GtfsSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim.MatsimSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.ConverterSink;
import ch.sbb.pfi.netzgrafikeditor.converter.core.NetworkGraphicConverter;
import ch.sbb.pfi.netzgrafikeditor.converter.core.NetworkGraphicConverterConfig;
import ch.sbb.pfi.netzgrafikeditor.converter.core.NetworkGraphicSource;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoVehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.io.csv.CsvInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.io.gtfs.GtfsScheduleWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.matsim.TransitScheduleXmlWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonFileReader;
import org.matsim.api.core.v01.Scenario;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class ConversionService {

    // TODO: Pass service config object instead
    public void convert(Path networkGraphicFile, Path outputDirectory, NetworkGraphicConverterConfig config, OutputFormat outputFormat, Path stopFacilityCsv) throws IOException {
        NetworkGraphicSource source = new JsonFileReader(networkGraphicFile);

        // TODO: Factor out method
        // setup csv infrastructure repository if provided
        InfrastructureRepository infrastructureRepository;
        if (stopFacilityCsv != null) {
            infrastructureRepository = new CsvInfrastructureRepository(stopFacilityCsv);
        } else {
            infrastructureRepository = new NoInfrastructureRepository();
        }

        NetworkGraphicConverter<?> converter = switch (outputFormat) {
            case GTFS -> {
                SupplyBuilder<GtfsSchedule> builder = new GtfsSupplyBuilder(infrastructureRepository,
                        new NoVehicleCircuitsPlanner(new NoRollingStockRepository()));
                ConverterSink<GtfsSchedule> sink = new GtfsScheduleWriter(outputDirectory, true);

                yield new NetworkGraphicConverter<>(config, source, builder, sink);
            }
            case MATSIM -> {
                SupplyBuilder<Scenario> builder = new MatsimSupplyBuilder(infrastructureRepository,
                        new NoVehicleCircuitsPlanner(new NoRollingStockRepository()));
                String baseFilename = networkGraphicFile.getFileName().toString();
                String filenameWithoutExtension = baseFilename.substring(0, baseFilename.lastIndexOf('.'));
                ConverterSink<Scenario> sink = new TransitScheduleXmlWriter(outputDirectory,
                        filenameWithoutExtension + ".");

                yield new NetworkGraphicConverter<>(config, source, builder, sink);
            }
        };

        converter.run();

    }
}
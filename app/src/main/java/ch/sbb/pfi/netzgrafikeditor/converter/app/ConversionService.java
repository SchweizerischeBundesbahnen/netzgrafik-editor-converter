package ch.sbb.pfi.netzgrafikeditor.converter.app;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.GtfsSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim.MatsimSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.ConverterSink;
import ch.sbb.pfi.netzgrafikeditor.converter.core.NetworkGraphicConverter;
import ch.sbb.pfi.netzgrafikeditor.converter.core.NetworkGraphicConverterConfig;
import ch.sbb.pfi.netzgrafikeditor.converter.core.NetworkGraphicSource;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoVehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.io.csv.CsvInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.io.csv.CsvRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.io.gtfs.GtfsScheduleWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.matsim.TransitScheduleXmlWriter;
import ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik.JsonFileReader;
import lombok.Builder;
import lombok.Value;
import org.matsim.api.core.v01.Scenario;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class ConversionService {

    private static InfrastructureRepository configureInfrastructureRepository(Path stopFacilityCsv) throws IOException {
        return stopFacilityCsv == null ? new NoInfrastructureRepository() : new CsvInfrastructureRepository(
                stopFacilityCsv);
    }

    private static RollingStockRepository configureRollingStockRepository(Path rollingStockCsv) throws IOException {
        return rollingStockCsv == null ? new NoRollingStockRepository() : new CsvRollingStockRepository(
                rollingStockCsv);
    }

    public void convert(Request request) throws IOException {
        NetworkGraphicSource source = new JsonFileReader(request.networkGraphicFile);

        InfrastructureRepository infrastructureRepository = configureInfrastructureRepository(request.stopFacilityCsv);
        RollingStockRepository rollingStockRepository = configureRollingStockRepository(request.rollingStockCsv);
        VehicleCircuitsPlanner vehicleCircuitsPlanner = new NoVehicleCircuitsPlanner(rollingStockRepository);

        NetworkGraphicConverter<?> converter = switch (request.outputFormat) {

            case GTFS -> {
                SupplyBuilder<GtfsSchedule> builder = new GtfsSupplyBuilder(infrastructureRepository,
                        vehicleCircuitsPlanner);
                ConverterSink<GtfsSchedule> sink = new GtfsScheduleWriter(request.outputDirectory, true);

                yield new NetworkGraphicConverter<>(request.converterConfig, source, builder, sink);
            }

            case MATSIM -> {
                SupplyBuilder<Scenario> builder = new MatsimSupplyBuilder(infrastructureRepository,
                        vehicleCircuitsPlanner);
                ConverterSink<Scenario> sink = new TransitScheduleXmlWriter(request.outputDirectory);

                yield new NetworkGraphicConverter<>(request.converterConfig, source, builder, sink);
            }

        };

        converter.run();
    }

    @Value
    @Builder
    public static class Request {
        Path networkGraphicFile;
        Path outputDirectory;
        NetworkGraphicConverterConfig converterConfig;
        OutputFormat outputFormat;
        Path stopFacilityCsv;
        Path rollingStockCsv;
    }
}
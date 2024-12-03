package ch.sbb.pfi.netzgrafikeditor.converter.app;

import ch.sbb.pfi.netzgrafikeditor.converter.core.NetworkGraphicConverterConfig;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@Component
@CommandLine.Command(name = "convert", mixinStandardHelpOptions = true, description = "Converts network graphics into timetables in various formats.", versionProvider = ConvertCommand.ManifestVersionProvider.class, footer = {""})
@RequiredArgsConstructor
public class ConvertCommand implements Callable<Integer> {

    private final BuildProperties buildProperties;
    private final ConversionService conversionService;

    // positional arguments
    @CommandLine.Parameters(index = "0", description = "The network graphic file to convert.")
    private Path networkGraphicFile;
    @CommandLine.Parameters(index = "1", description = "The output directory for the converted timetable.")

    // converter configuration
    private Path outputDirectory;
    @CommandLine.Option(names = {"-v", "--validation"}, description = "Validation strategy (SKIP_VALIDATION, WARN_ON_ISSUES, FAIL_ON_ISSUES, FIX_ISSUES).", defaultValue = "WARN_ON_ISSUES")
    private ValidationStrategy validationStrategy;
    @CommandLine.Option(names = {"-t", "--train-names"}, description = "Use train names as route or line IDs (true/false).", defaultValue = "false")
    private boolean useTrainNamesAsIds;
    @CommandLine.Option(names = {"-s", "--service-day-start"}, description = "Service day start time (HH:mm).", converter = ServiceDayTimeConverter.class, defaultValue = "04:00")
    private ServiceDayTime serviceDayStart;
    @CommandLine.Option(names = {"-e", "--service-day-end"}, description = "Service day end time (HH:mm).", converter = ServiceDayTimeConverter.class, defaultValue = "25:00")
    private ServiceDayTime serviceDayEnd;

    // format and repositories
    @CommandLine.Option(names = {"-f", "--format"}, description = "Output format (GTFS or MATSim).", defaultValue = "GTFS")
    private OutputFormat outputFormat;
    @CommandLine.Option(names = {"-i", "--stop-facility-csv"}, description = "File which contains the coordinates of the stop facilities.")
    private Path stopFacilityCsv;

    @Override
    public Integer call() throws Exception {
        conversionService.convert(networkGraphicFile, outputDirectory, deriveNetworkGraphicConfig(), outputFormat,
                stopFacilityCsv);
        return 0;
    }

    private NetworkGraphicConverterConfig deriveNetworkGraphicConfig() {
        return NetworkGraphicConverterConfig.builder()
                .validationStrategy(validationStrategy)
                .useTrainNamesAsIds(useTrainNamesAsIds)
                .serviceDayStart(serviceDayStart)
                .serviceDayEnd(serviceDayEnd)
                .build();
    }

    class ManifestVersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[]{String.format("%s %s", buildProperties.getArtifact(), buildProperties.getVersion())};
        }
    }

    class FooterProvider implements CommandLine.IHelpSectionRenderer {

        private static final String TEMPLATE = """
                
                %s  Copyright (C) 2024  Swiss Federal Railways SBB AG
                
                This program is free software: you can redistribute it and/or modify it under
                the terms of the GNU General Public License as published by the Free Software
                Foundation, either version 3 of the License, or (at your option) any later
                version.
                
                This program is distributed in the hope that it will be useful, but WITHOUT ANY
                WARRANTY; without even the implied warranty ofmMERCHANTABILITY or FITNESS FOR A
                PARTICULAR PURPOSE. See the GNU General Public License for more details.
                """;

        @Override
        public String render(CommandLine.Help help) {
            return String.format(TEMPLATE, buildProperties.getArtifact());
        }
    }
}

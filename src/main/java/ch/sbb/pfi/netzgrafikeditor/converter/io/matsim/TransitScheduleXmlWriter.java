package ch.sbb.pfi.netzgrafikeditor.converter.io.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.core.ConverterSink;
import lombok.RequiredArgsConstructor;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.MatsimVehicleWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RequiredArgsConstructor
public class TransitScheduleXmlWriter implements ConverterSink {

    private static final String CONFIG_FILE = "config.xml";
    private static final String NETWORK_FILE = "network.xml.gz";
    private static final String TRANSIT_SCHEDULE_FILE = "transitSchedule.xml.gz";
    private static final String TRANSIT_VEHICLE_FILE = "transitVehicles.xml.gz";

    private final Scenario scenario;
    private final Path directory;
    private final String prefix;

    @Override
    public void save() throws IOException {
        Files.createDirectories(directory);

        writeFile(CONFIG_FILE, new ConfigWriter(scenario.getConfig())::write);
        writeFile(NETWORK_FILE, new NetworkWriter(scenario.getNetwork())::write);
        writeFile(TRANSIT_SCHEDULE_FILE, new TransitScheduleWriter(scenario.getTransitSchedule())::writeFile);
        writeFile(TRANSIT_VEHICLE_FILE, new MatsimVehicleWriter(scenario.getTransitVehicles())::writeFile);
    }

    private void writeFile(String fileName, FileWriterAction writerAction) throws IOException {
        Path filePath = directory.resolve(prefix + fileName);
        writerAction.write(filePath.toString());
    }

    @FunctionalInterface
    private interface FileWriterAction {
        void write(String filePath) throws IOException;
    }
}

package ch.sbb.pfi.netzgrafikeditor.converter.io.csv;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class CsvRollingStockRepository implements RollingStockRepository {

    private static final double KMH_TO_MS = 3.6;

    private final Map<String, VehicleTypeInfo> vehicleTypeInfos = new HashMap<>();

    public CsvRollingStockRepository(Path filePath) throws IOException {
        log.info("Reading vehicle type info CSV file: {}", filePath);

        try (FileInputStream fileInputStream = new FileInputStream(
                filePath.toFile()); BOMInputStream bomInputStream = BOMInputStream.builder()
                .setInputStream(fileInputStream)
                .setByteOrderMarks(ByteOrderMark.UTF_8)
                .get(); InputStreamReader reader = new InputStreamReader(bomInputStream, StandardCharsets.UTF_8)) {

            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setIgnoreHeaderCase(true).setTrim(true).build();

            try (CSVParser csvParser = new CSVParser(reader, format)) {
                csvParser.forEach(record -> {

                    String category = record.get("category");
                    String vehicleTypeId = record.get("vehicle_type_id");
                    int seats = Integer.parseInt(record.get("seats"));
                    int standingRoom = Integer.parseInt(record.get("standing_room"));
                    double length = Double.parseDouble(record.get("length"));
                    double maxVelocity = Double.parseDouble(record.get("max_velocity")) / KMH_TO_MS;

                    vehicleTypeInfos.put(category,
                            new VehicleTypeInfo(vehicleTypeId, seats, standingRoom, length, maxVelocity,
                                    new HashMap<>()));
                });
            }
        }
    }

    private VehicleTypeInfo getVehicleTypeInfo(String category) {
        VehicleTypeInfo vehicleTypeInfo = vehicleTypeInfos.get(category);
        if (vehicleTypeInfo == null) {
            throw new IllegalArgumentException("No vehicle type entry not found in CSV for " + category);
        }

        return vehicleTypeInfo;
    }

    @Override
    public VehicleTypeInfo getVehicleType(String category) {
        return getVehicleTypeInfo(category);
    }
}


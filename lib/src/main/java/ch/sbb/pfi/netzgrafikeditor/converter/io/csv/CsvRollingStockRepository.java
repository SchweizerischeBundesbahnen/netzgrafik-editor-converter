package ch.sbb.pfi.netzgrafikeditor.converter.io.csv;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransportMode;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

@Slf4j
public class CsvRollingStockRepository extends CsvRepository<VehicleTypeInfo> implements RollingStockRepository {

    private static final double KMH_TO_MS = 3.6;

    public CsvRollingStockRepository(Path filePath) throws IOException {
        super(filePath, record -> {
            String category = record.get("category");
            TransportMode transportMode = TransportMode.valueOf(record.get("transport_mode").toUpperCase());
            String vehicleTypeId = record.get("vehicle_type_id");
            int seats = Integer.parseInt(record.get("seats"));
            int standingRoom = Integer.parseInt(record.get("standing_room"));
            double length = Double.parseDouble(record.get("length"));
            double maxVelocity = Double.parseDouble(record.get("max_velocity")) / KMH_TO_MS;

            return new Entry<>(category,
                    new VehicleTypeInfo(vehicleTypeId, transportMode, seats, standingRoom, length, maxVelocity,
                            new HashMap<>()));
        });
    }

    @Override
    public VehicleTypeInfo getVehicleType(String category) {
        return getEntity(category);
    }
}

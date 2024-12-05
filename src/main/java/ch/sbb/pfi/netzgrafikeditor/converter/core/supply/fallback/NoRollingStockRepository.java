package ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;

import java.util.Map;

public class NoRollingStockRepository implements RollingStockRepository {

    public static final int DEFAULT_SEATS = 9999;
    public static final double DEFAULT_LENGTH = 100.; // meters
    public static final double DEFAULT_MAX_VELOCITY = 120 / 3.6; // meters per second
    private static final int DEFAULT_STANDING_ROOM = 9999;

    @Override
    public VehicleTypeInfo getVehicleType(String category) {
        return new VehicleTypeInfo(category, DEFAULT_SEATS, DEFAULT_STANDING_ROOM, DEFAULT_LENGTH, DEFAULT_MAX_VELOCITY,
                Map.of());
    }

}

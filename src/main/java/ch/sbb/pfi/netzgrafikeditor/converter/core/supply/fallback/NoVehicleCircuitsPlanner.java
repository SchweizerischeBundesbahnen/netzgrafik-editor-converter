package ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.DepartureInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteDirection;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoVehicleCircuitsPlanner implements VehicleCircuitsPlanner {

    private final List<DepartureInfo> departures = new ArrayList<>();

    @Override
    public void register(DepartureInfo departureInfo) {
        departures.add(departureInfo);
    }

    @Override
    public List<VehicleAllocation> plan() {
        Map<String, Integer> vehicleCounts = new HashMap<>();
        Map<String, EnumMap<RouteDirection, Integer>> departureCounts = new HashMap<>();

        return departures.stream().sorted(Comparator.comparing(DepartureInfo::getTime)).map(departureInfo -> {
            VehicleTypeInfo vehicleTypeInfo = departureInfo.getTransitLine().getVehicleTypeInfo();

            // update vehicle counts and departure counts
            int vehicleCount = vehicleCounts.merge(vehicleTypeInfo.getId(), 1, Integer::sum);
            EnumMap<RouteDirection, Integer> lineDepartureCounts = departureCounts.computeIfAbsent(
                    vehicleTypeInfo.getId(), k -> new EnumMap<>(RouteDirection.class));
            int departureCount = lineDepartureCounts.merge(departureInfo.getDirection(), 1, Integer::sum);

            return new VehicleAllocation(String.format("%s_%s_%s", departureInfo.getTransitLine().getId(),
                    departureInfo.getDirection().name(), departureCount), departureInfo,
                    new VehicleInfo(String.format("%s_%d", vehicleTypeInfo.getId(), vehicleCount), vehicleTypeInfo));
        }).toList();
    }

}

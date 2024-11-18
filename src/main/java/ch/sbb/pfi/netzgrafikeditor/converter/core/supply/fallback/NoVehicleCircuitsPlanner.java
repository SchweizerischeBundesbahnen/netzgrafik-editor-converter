package ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.DepartureInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class NoVehicleCircuitsPlanner implements VehicleCircuitsPlanner {

    private final RollingStockRepository rollingStockRepository;

    private final Map<String, VehicleTypeInfo> vehicleTypeInfos = new HashMap<>();
    private final List<DepartureInfo> departureInfos = new ArrayList<>();

    @Override
    public void register(DepartureInfo departureInfo) {
        departureInfos.add(departureInfo);
    }

    @Override
    public List<VehicleAllocation> plan() {
        Map<String, Integer> vehicleCounts = new HashMap<>();
        Map<String, Integer> departureCounts = new HashMap<>();

        return departureInfos.stream().sorted(Comparator.comparing(DepartureInfo::getTime)).map(departureInfo -> {

            // get vehicle type based on product
            String productId = departureInfo.getTransitRouteInfo().getTransitLineInfo().getProductId();
            VehicleTypeInfo vehicleTypeInfo = vehicleTypeInfos.get(productId);
            if (vehicleTypeInfo == null) {
                vehicleTypeInfo = rollingStockRepository.getVehicleType(productId);
                vehicleTypeInfos.put(productId, vehicleTypeInfo);
            }

            // update vehicle counts and departure counts
            int vehicleCount = vehicleCounts.merge(vehicleTypeInfo.getId(), 1, Integer::sum);
            int departureCount = departureCounts.merge(departureInfo.getTransitRouteInfo().getId(), 1, Integer::sum);

            return new VehicleAllocation(
                    String.format("%s_%s", departureInfo.getTransitRouteInfo().getId(), departureCount), departureInfo,
                    new VehicleInfo(String.format("%s_%d", vehicleTypeInfo.getId(), vehicleCount), vehicleTypeInfo));
        }).toList();
    }

}

package ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.BaseSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.DepartureInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;
import lombok.extern.slf4j.Slf4j;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MatsimSupplyBuilder extends BaseSupplyBuilder<Scenario> {

    private final Map<String, TransitRoute> transitRoutes = new HashMap<>();
    private final MatsimSupplyFactory factory;
    private final InfrastructureBuilder infrastructureBuilder;
    private final Scenario scenario;

    public MatsimSupplyBuilder(InfrastructureRepository infrastructureRepository, RollingStockRepository rollingStockRepository, VehicleCircuitsPlanner vehicleCircuitsPlanner) {
        super(infrastructureRepository, rollingStockRepository, vehicleCircuitsPlanner);
        this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        factory = new MatsimSupplyFactory(scenario);
        infrastructureBuilder = new InfrastructureBuilder(scenario, factory, infrastructureRepository);
    }

    @Override
    protected void buildStopFacility(StopFacilityInfo stopFacilityInfo) {
        infrastructureBuilder.buildTransitStopFacility(stopFacilityInfo);
    }

    @Override
    protected void buildTransitRoute(TransitRouteContainer transitRouteContainer) {
        // store transit route to add departures in later
        transitRoutes.put(transitRouteContainer.transitRouteInfo().getId(),
                infrastructureBuilder.buildTransitRoute(transitRouteContainer.transitRouteInfo(),
                        transitRouteContainer.routeElements()));
    }

    @Override
    protected void buildDeparture(VehicleAllocation vehicleAllocation) {
        log.debug("Adding departure {} (line: {} route: {}, vehicle type: {} vehicle: {})",
                vehicleAllocation.getDepartureId(),
                vehicleAllocation.getDepartureInfo().getTransitRouteInfo().getTransitLineInfo().getId(),
                vehicleAllocation.getDepartureInfo().getTransitRouteInfo().getId(),
                vehicleAllocation.getVehicleInfo().getVehicleTypeInfo().getId(),
                vehicleAllocation.getVehicleInfo().getId());

        VehicleTypeInfo vehicleTypeInfo = vehicleAllocation.getVehicleInfo().getVehicleTypeInfo();
        VehicleType vehicleType = factory.getOrCreateVehicleType(vehicleTypeInfo.getId(), vehicleTypeInfo.getLength(),
                vehicleTypeInfo.getMaxVelocity(), vehicleTypeInfo.getSeats(), vehicleTypeInfo.getStandingRoom(),
                vehicleTypeInfo.getAttributes());
        Vehicle vehicle = factory.getOrCreateVehicle(vehicleType, vehicleAllocation.getVehicleInfo().getId());

        DepartureInfo departureInfo = vehicleAllocation.getDepartureInfo();
        Departure departure = factory.createDeparture(vehicleAllocation.getDepartureId(),
                departureInfo.getTime().toSecondOfDay());
        departure.setVehicleId(vehicle.getId());

        // add departure to the corresponding transit route
        transitRoutes.get(departureInfo.getTransitRouteInfo().getId()).addDeparture(departure);
    }

    @Override
    protected Scenario getResult() {
        return scenario;
    }

}

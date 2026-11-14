package ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.BaseSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.ConnectionInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.DepartureInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
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
    private boolean connectionsLeftOut;

    /**
     * Creates a MATSim supply builder that handles tracks of the infrastructure repository that do not form a
     * continuous path between their stops according to the validation strategy.
     */
    public MatsimSupplyBuilder(InfrastructureRepository infrastructureRepository, RollingStockRepository rollingStockRepository, VehicleCircuitsPlanner vehicleCircuitsPlanner, ValidationStrategy validationStrategy) {
        super(infrastructureRepository, rollingStockRepository, vehicleCircuitsPlanner);
        this.scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        factory = new MatsimSupplyFactory(scenario);
        infrastructureBuilder = new InfrastructureBuilder(factory, infrastructureRepository, validationStrategy);
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

    /**
     * Leaves the connection out: a MATSim transit schedule relates transfers to stop facilities only, not to particular
     * trips, so the connection cannot be expressed.
     */
    @Override
    protected void buildConnection(ConnectionInfo connectionInfo) {
        if (!connectionsLeftOut) {
            log.info("MATSim has no transfers between particular trips, leaving out the connections");
            connectionsLeftOut = true;
        }
        log.debug("Leaving out connection at {} from {} at {} to {} at {}",
                connectionInfo.getStopFacilityInfo().getId(), connectionInfo.getFromTransitRouteInfo().getId(),
                connectionInfo.getFromDeparture(), connectionInfo.getToTransitRouteInfo().getId(),
                connectionInfo.getToDeparture());
    }

    /**
     * Fails: without transfers between particular trips, a stop that forbids all other transfers would forbid every
     * transfer, so the restriction cannot be expressed.
     */
    @Override
    protected void buildTransferRestriction(StopFacilityInfo stopFacilityInfo) {
        throw new UnsupportedOperationException(String.format(
                "MATSim cannot restrict the transfers at stop %s to particular trips; convert to a format that can, e.g. GTFS",
                stopFacilityInfo.getId()));
    }

    @Override
    protected Scenario getResult() {
        infrastructureBuilder.handleTrackIssues();
        return scenario;
    }

}

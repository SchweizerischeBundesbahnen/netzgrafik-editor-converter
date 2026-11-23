package ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopAccess;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoVehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Line x runs from a to b, where nobody boards; line y runs from b to c, where nobody alights before departing.
 */
class MatsimSupplyBuilderConnectionsTest {

    private SupplyBuilder<Scenario> builder;

    private static TransitRouteStop getStop(Scenario scenario, String lineId, String routeId, int index) {
        TransitRoute route = scenario.getTransitSchedule()
                .getTransitLines()
                .get(Id.create(lineId, TransitLine.class))
                .getRoutes()
                .get(Id.create(routeId, TransitRoute.class));
        return route.getStops().get(index);
    }

    @BeforeEach
    void setUp() {
        RollingStockRepository rollingStockRepository = new NoRollingStockRepository();
        builder = new MatsimSupplyBuilder(new NoInfrastructureRepository(), rollingStockRepository,
                new NoVehicleCircuitsPlanner(rollingStockRepository), ValidationStrategy.WARN_ON_ISSUES);
        builder.addStopFacility("a", "Stop A", 0, 0)
                .addStopFacility("b", "Stop B", 1, 0)
                .addStopFacility("c", "Stop C", 2, 0)
                .addTransitLine("x", "IC")
                .addTransitRoute("x_forward", "x", "a", Duration.ZERO, StopAccess.NONE)
                .addRouteStop("x_forward", "b", Duration.ofMinutes(10), Duration.ZERO, StopAccess.ALIGHTING_ONLY)
                .addTransitLine("y", "IC")
                .addTransitRoute("y_forward", "y", "b", Duration.ZERO, StopAccess.BOARDING_ONLY)
                .addRouteStop("y_forward", "c", Duration.ofMinutes(10), Duration.ZERO,
                        StopAccess.BOARDING_AND_ALIGHTING)
                .addDeparture("x_forward", ServiceDayTime.of(7, 0, 0))
                .addDeparture("y_forward", ServiceDayTime.of(7, 10, 0));
    }

    @Test
    void stopAccessSetsBoardingAndAlighting() {
        Scenario scenario = builder.build();

        TransitRouteStop none = getStop(scenario, "x", "x_forward", 0);
        assertFalse(none.isAllowBoarding());
        assertFalse(none.isAllowAlighting());

        TransitRouteStop alighting = getStop(scenario, "x", "x_forward", 1);
        assertFalse(alighting.isAllowBoarding());
        assertTrue(alighting.isAllowAlighting());

        TransitRouteStop boarding = getStop(scenario, "y", "y_forward", 0);
        assertTrue(boarding.isAllowBoarding());
        assertFalse(boarding.isAllowAlighting());

        TransitRouteStop regular = getStop(scenario, "y", "y_forward", 1);
        assertTrue(regular.isAllowBoarding());
        assertTrue(regular.isAllowAlighting());
    }

    @Test
    void connectionsAreLeftOut() {
        builder.addConnection("b", "x_forward", ServiceDayTime.of(7, 0, 0), "y_forward", ServiceDayTime.of(7, 10, 0));

        assertDoesNotThrow(builder::build);
    }

    @Test
    void forbiddenTransfersCannotBeExpressed() {
        builder.forbidTransfers("b");

        assertThrows(UnsupportedOperationException.class, builder::build);
    }

}

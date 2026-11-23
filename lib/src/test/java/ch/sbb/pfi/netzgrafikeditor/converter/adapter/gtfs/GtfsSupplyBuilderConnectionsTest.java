package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.StopTime;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Transfer;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.TransferType;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopAccess;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.SupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoInfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoRollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback.NoVehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Line x runs from a to b at 06:00 and 07:00, line y from b to c at 07:10; the trip of x at 07:00 connects to y at b.
 */
class GtfsSupplyBuilderConnectionsTest {

    private SupplyBuilder<GtfsSchedule> builder;

    private static StopTime getStopTime(GtfsSchedule schedule, String tripId, String stopId) {
        return schedule.getStopTimes()
                .stream()
                .filter(stopTime -> stopTime.getTripId().equals(tripId) && stopTime.getStopId().equals(stopId))
                .findFirst()
                .orElseThrow();
    }

    @BeforeEach
    void setUp() {
        RollingStockRepository rollingStockRepository = new NoRollingStockRepository();
        builder = new GtfsSupplyBuilder(new NoInfrastructureRepository(), rollingStockRepository,
                new NoVehicleCircuitsPlanner(rollingStockRepository)).addStopFacility("a", "Stop A", 0, 0)
                .addStopFacility("b", "Stop B", 1, 0)
                .addStopFacility("c", "Stop C", 2, 0)
                .addTransitLine("x", "IC")
                .addTransitRoute("x_forward", "x", "a", Duration.ZERO, StopAccess.BOARDING_AND_ALIGHTING)
                .addRouteStop("x_forward", "b", Duration.ofMinutes(10), Duration.ZERO, StopAccess.ALIGHTING_ONLY)
                .addTransitLine("y", "IC")
                .addTransitRoute("y_forward", "y", "b", Duration.ZERO, StopAccess.BOARDING_ONLY)
                .addRouteStop("y_forward", "c", Duration.ofMinutes(10), Duration.ZERO,
                        StopAccess.BOARDING_AND_ALIGHTING)
                .addDeparture("x_forward", ServiceDayTime.of(6, 0, 0))
                .addDeparture("x_forward", ServiceDayTime.of(7, 0, 0))
                .addDeparture("y_forward", ServiceDayTime.of(7, 10, 0));
    }

    @Test
    void connectionIsATimedTransferBetweenTheTrips() {
        GtfsSchedule schedule = builder.addConnection("b", "x_forward", ServiceDayTime.of(7, 0, 0), "y_forward",
                ServiceDayTime.of(7, 10, 0)).build();

        // the trips are numbered by their departure, so the trip of x at 07:00 is the second
        assertEquals(List.of(Transfer.builder()
                .fromStopId("b")
                .toStopId("b")
                .fromTripId("x_forward_2")
                .toTripId("y_forward_1")
                .transferType(TransferType.TIMED)
                .build()), schedule.getTransfers());
    }

    @Test
    void forbiddenTransfersAreNotPossibleAtTheStop() {
        GtfsSchedule schedule = builder.forbidTransfers("b").build();

        assertEquals(List.of(
                        Transfer.builder().fromStopId("b").toStopId("b").transferType(TransferType.NOT_POSSIBLE).build()),
                schedule.getTransfers());
    }

    @Test
    void noTransfersWithoutConnectionsOrRestrictions() {
        assertTrue(builder.build().getTransfers().isEmpty());
    }

    @Test
    void stopAccessSetsPickupAndDropOff() {
        GtfsSchedule schedule = builder.build();

        StopTime alighting = getStopTime(schedule, "x_forward_1", "b");
        assertEquals(StopTime.NOT_AVAILABLE, alighting.getPickupType());
        assertNull(alighting.getDropOffType());

        StopTime boarding = getStopTime(schedule, "y_forward_1", "b");
        assertNull(boarding.getPickupType());
        assertEquals(StopTime.NOT_AVAILABLE, boarding.getDropOffType());

        StopTime regular = getStopTime(schedule, "x_forward_1", "a");
        assertNull(regular.getPickupType());
        assertNull(regular.getDropOffType());
    }

}

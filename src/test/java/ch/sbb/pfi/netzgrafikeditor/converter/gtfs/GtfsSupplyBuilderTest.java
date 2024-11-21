package ch.sbb.pfi.netzgrafikeditor.converter.gtfs;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.Coordinate;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.DepartureInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitRouteInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.gtfs.model.GtfsSchedule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GtfsSupplyBuilderTest {

    @Mock
    private InfrastructureRepository infrastructureRepository;

    @Mock
    private VehicleCircuitsPlanner vehicleCircuitsPlanner;

    @InjectMocks
    private GtfsSupplyBuilder gtfsSupplyBuilder;

    @BeforeEach
    void setUp() {
        when(infrastructureRepository.getStopFacility(eq("a"), any(Double.class), any(Double.class))).thenReturn(
                new StopFacilityInfo("a", new Coordinate(1, 1)));
        when(infrastructureRepository.getStopFacility(eq("b"), any(Double.class), any(Double.class))).thenReturn(
                new StopFacilityInfo("b", new Coordinate(2, 2)));
        when(infrastructureRepository.getStopFacility(eq("c"), any(Double.class), any(Double.class))).thenReturn(
                new StopFacilityInfo("c", new Coordinate(3, 3)));
        when(infrastructureRepository.getStopFacility(eq("d"), any(Double.class), any(Double.class))).thenReturn(
                new StopFacilityInfo("d", new Coordinate(4, 4)));

        TransitRouteInfo transitRouteInfo = new TransitRouteInfo("routeId", null);
        DepartureInfo departureInfo = new DepartureInfo(transitRouteInfo, LocalTime.MIN);
        VehicleAllocation vehicleAllocation = new VehicleAllocation(null, departureInfo, null);

        when(vehicleCircuitsPlanner.plan()).thenReturn(List.of(vehicleAllocation));
    }

    @Test
    void testBuild() {
        GtfsSchedule schedule = gtfsSupplyBuilder.addStopFacility("a", 0, 0)
                .addStopFacility("b", 1, 0)
                .addStopFacility("c", 2, 0)
                .addStopFacility("d", 3, 0)
                .addTransitLine("lineId", "IC")
                .addTransitRoute("routeId", "lineId", "a", Duration.of(5, ChronoUnit.MINUTES))
                .addRoutePass("routeId", "b")
                .addRouteStop("routeId", "c", Duration.of(10, ChronoUnit.MINUTES), Duration.of(5, ChronoUnit.MINUTES))
                .addRouteStop("routeId", "d", Duration.of(10, ChronoUnit.MINUTES), Duration.of(5, ChronoUnit.MINUTES))
                .addDeparture("routeId", LocalTime.MIN)
                .build();

        assertNotNull(schedule);
        assertEquals(1, schedule.getAgencies().size());
        assertEquals(4, schedule.getStops().size());
        assertEquals(1, schedule.getRoutes().size());
        assertEquals(1, schedule.getTrips().size());
        assertEquals(3, schedule.getStopTimes().size());
        assertEquals(1, schedule.getCalendars().size());
    }

}
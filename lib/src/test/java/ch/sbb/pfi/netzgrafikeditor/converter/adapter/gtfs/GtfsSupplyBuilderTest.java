package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.DepartureInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitLineInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitRouteInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransportMode;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.Coordinate;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

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
    private RollingStockRepository rollingStockRepository;

    @Mock
    private VehicleCircuitsPlanner vehicleCircuitsPlanner;

    @InjectMocks
    private GtfsSupplyBuilder gtfsSupplyBuilder;

    @BeforeEach
    void setUp() {
        when(infrastructureRepository.getStopFacility(eq("a"), any(String.class), any(Double.class),
                any(Double.class))).thenReturn(new StopFacilityInfo("a", "Stop A", new Coordinate(1, 1)));
        when(infrastructureRepository.getStopFacility(eq("b"), any(String.class), any(Double.class),
                any(Double.class))).thenReturn(new StopFacilityInfo("b", "Stop B", new Coordinate(2, 2)));
        when(infrastructureRepository.getStopFacility(eq("c"), any(String.class), any(Double.class),
                any(Double.class))).thenReturn(new StopFacilityInfo("c", "Stop C", new Coordinate(3, 3)));
        when(infrastructureRepository.getStopFacility(eq("d"), any(String.class), any(Double.class),
                any(Double.class))).thenReturn(new StopFacilityInfo("d", "Stop D", new Coordinate(4, 4)));

        TransitLineInfo transitLineInfo = new TransitLineInfo("lineId", null, TransportMode.RAIL);
        TransitRouteInfo transitRouteInfo = new TransitRouteInfo("routeId", transitLineInfo);
        DepartureInfo departureInfo = new DepartureInfo(transitRouteInfo, ServiceDayTime.NOON);
        VehicleTypeInfo vehicleTypeInfo = new VehicleTypeInfo("train", TransportMode.RAIL, 100, 150, 200, 90 * 3.6,
                Map.of());
        VehicleAllocation vehicleAllocation = new VehicleAllocation(null, departureInfo,
                new VehicleInfo("train1", vehicleTypeInfo));

        when(vehicleCircuitsPlanner.plan()).thenReturn(List.of(vehicleAllocation));
        when(rollingStockRepository.getVehicleType(any())).thenReturn(vehicleTypeInfo);
    }

    @Test
    void testBuild() {
        GtfsSchedule schedule = gtfsSupplyBuilder.addStopFacility("a", "Stop A", 0, 0)
                .addStopFacility("b", "Stop B", 1, 0)
                .addStopFacility("c", "Stop C", 2, 0)
                .addStopFacility("d", "Stop D", 3, 0)
                .addTransitLine("lineId", "rail")
                .addTransitRoute("routeId", "lineId", "a", Duration.of(5, ChronoUnit.MINUTES))
                .addRoutePass("routeId", "b")
                .addRouteStop("routeId", "c", Duration.of(10, ChronoUnit.MINUTES), Duration.of(5, ChronoUnit.MINUTES))
                .addRouteStop("routeId", "d", Duration.of(10, ChronoUnit.MINUTES), Duration.of(5, ChronoUnit.MINUTES))
                .addDeparture("routeId", ServiceDayTime.NOON)
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
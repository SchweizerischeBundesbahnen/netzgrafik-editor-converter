package ch.sbb.pfi.netzgrafikeditor.converter.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.supply.Coordinate;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.DepartureInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.RouteDirection;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.TrackSegmentInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.TransitLineInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.VehicleInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.VehicleTypeInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicles;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatsimSupplyBuilderTest {

    private Scenario scenario;
    @Mock
    private InfrastructureRepository infrastructureRepository;
    @Mock
    private RollingStockRepository rollingStockRepository;
    @Mock
    private VehicleCircuitsPlanner vehicleCircuitsPlanner;
    private MatsimSupplyBuilder matsimSupplyBuilder;

    private static List<VehicleAllocation> mockVehicleAllocations() {
        VehicleTypeInfo vehicleTypeInfo1 = new VehicleTypeInfo("vehicleType1", 100, 200.0, 50.0, Map.of());
        VehicleTypeInfo vehicleTypeInfo2 = new VehicleTypeInfo("vehicleType2", 150, 300.0, 60.0, Map.of());

        VehicleInfo vehicleInfo1 = new VehicleInfo("vehicle1", vehicleTypeInfo1);
        VehicleInfo vehicleInfo2 = new VehicleInfo("vehicle2", vehicleTypeInfo1);
        VehicleInfo vehicleInfo3 = new VehicleInfo("vehicle3", vehicleTypeInfo2);
        VehicleInfo vehicleInfo4 = new VehicleInfo("vehicle4", vehicleTypeInfo2);

        DepartureInfo departureInfo1 = new DepartureInfo(new TransitLineInfo("lineSimple", vehicleTypeInfo1),
                RouteDirection.FORWARD, Default.SERVICE_DAY_START.plusMinutes(0));

        DepartureInfo departureInfo2 = new DepartureInfo(new TransitLineInfo("lineSimple", vehicleTypeInfo1),
                RouteDirection.FORWARD, Default.SERVICE_DAY_START.plusMinutes(5));

        DepartureInfo departureInfo3 = new DepartureInfo(new TransitLineInfo("lineSimple", vehicleTypeInfo1),
                RouteDirection.FORWARD, Default.SERVICE_DAY_START.plusMinutes(10));

        DepartureInfo departureInfo4 = new DepartureInfo(new TransitLineInfo("lineSimple", vehicleTypeInfo2),
                RouteDirection.REVERSE, Default.SERVICE_DAY_START.plusMinutes(1));

        DepartureInfo departureInfo5 = new DepartureInfo(new TransitLineInfo("lineSimple", vehicleTypeInfo2),
                RouteDirection.REVERSE, Default.SERVICE_DAY_START.plusMinutes(6));

        DepartureInfo departureInfo6 = new DepartureInfo(new TransitLineInfo("lineSimple", vehicleTypeInfo2),
                RouteDirection.REVERSE, Default.SERVICE_DAY_START.plusMinutes(11));

        VehicleAllocation allocation1 = new VehicleAllocation("departure1", departureInfo1, vehicleInfo1);
        VehicleAllocation allocation2 = new VehicleAllocation("departure2", departureInfo2, vehicleInfo2);
        VehicleAllocation allocation3 = new VehicleAllocation("departure3", departureInfo3, vehicleInfo3);
        VehicleAllocation allocation4 = new VehicleAllocation("departure4", departureInfo4, vehicleInfo4);
        VehicleAllocation allocation5 = new VehicleAllocation("departure5", departureInfo5, vehicleInfo1);
        VehicleAllocation allocation6 = new VehicleAllocation("departure6", departureInfo6, vehicleInfo2);

        return List.of(allocation1, allocation2, allocation3, allocation4, allocation5, allocation6);
    }

    @BeforeEach
    void setUp() {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        matsimSupplyBuilder = new MatsimSupplyBuilder(scenario, infrastructureRepository, rollingStockRepository,
                vehicleCircuitsPlanner);
    }

    /**
     * Helper method to mock track segments for both forward and reverse directions.
     */
    private void mockTrackSegments(Stop stopA, Stop stopB, Segments segments) {
        when(infrastructureRepository.getTrack(eq(stopA.getStopFacilityInfo()), eq(stopB.getStopFacilityInfo()),
                any(TransitLineInfo.class))).thenReturn(segments.getSegments());
    }

    /**
     * A -- B -- (C) -- D
     */
    @Test
    void addTransitLine_simple() {
        // mock infrastructure
        when(infrastructureRepository.getStopFacility("A")).thenReturn(Stop.A.getStopFacilityInfo());
        when(infrastructureRepository.getStopFacility("B")).thenReturn(Stop.B.getStopFacilityInfo());
        when(infrastructureRepository.getStopFacility("C")).thenReturn(Stop.C.getStopFacilityInfo());
        when(infrastructureRepository.getStopFacility("D")).thenReturn(Stop.D.getStopFacilityInfo());

        // mock track segments: forward
        mockTrackSegments(Stop.A, Stop.B, Segments.A_B);
        mockTrackSegments(Stop.B, Stop.C, Segments.B_C);
        mockTrackSegments(Stop.C, Stop.D, Segments.C_D);
        // reverse
        mockTrackSegments(Stop.D, Stop.C, Segments.D_C);
        mockTrackSegments(Stop.C, Stop.B, Segments.C_B);
        mockTrackSegments(Stop.B, Stop.A, Segments.B_A);

        // mock the plan method
        when(vehicleCircuitsPlanner.plan()).thenReturn(mockVehicleAllocations());

        // act
        matsimSupplyBuilder.addStopFacility("A")
                .addStopFacility("B")
                .addStopFacility("C")
                .addStopFacility("D")
                .addTransitLine("lineSimple", "vehicleType", "A", Default.DWELL_TIME)
                .addRouteStop("lineSimple", "B", Default.TRAVEL_TIME, Default.DWELL_TIME)
                .addRoutePass("lineSimple", "C")
                .addRouteStop("lineSimple", "D", Default.TRAVEL_TIME.plus(Default.TRAVEL_TIME), Default.DWELL_TIME)
                .addDeparture("lineSimple", RouteDirection.FORWARD, Default.SERVICE_DAY_START.plusMinutes(0))
                .addDeparture("lineSimple", RouteDirection.FORWARD, Default.SERVICE_DAY_START.plusMinutes(5))
                .addDeparture("lineSimple", RouteDirection.FORWARD, Default.SERVICE_DAY_START.plusMinutes(10))
                .addDeparture("lineSimple", RouteDirection.REVERSE, Default.SERVICE_DAY_START.plusMinutes(1))
                .addDeparture("lineSimple", RouteDirection.REVERSE, Default.SERVICE_DAY_START.plusMinutes(6))
                .addDeparture("lineSimple", RouteDirection.REVERSE, Default.SERVICE_DAY_START.plusMinutes(11))
                .build();

        // assert
        Network network = scenario.getNetwork();
        assertNotNull(network);
        assertEquals(16, network.getNodes().size(), "Expected 16 nodes in the network");
        assertEquals(16, network.getLinks().size(), "Expected 16 links in the network");

        TransitSchedule schedule = scenario.getTransitSchedule();
        assertNotNull(schedule);
        assertEquals(4, schedule.getFacilities().size(), "Expected 4 stop facilities");
        assertEquals(1, schedule.getTransitLines().size(), "Expected 1 transit line in the schedule");
        assertTrue(schedule.getTransitLines().containsKey(Id.create("lineSimple", TransitLine.class)),
                "Transit line 'lineSimple' should be present");
        assertEquals(2, schedule.getTransitLines().get(Id.create("lineSimple", TransitLine.class)).getRoutes().size(),
                "Expected 2 transit routes for 'lineSimple'");

        Vehicles vehicles = scenario.getTransitVehicles();
        assertNotNull(vehicles);
        assertEquals(2, vehicles.getVehicleTypes().size());
        assertEquals(4, vehicles.getVehicles().size());
    }

    @RequiredArgsConstructor
    @Getter
    enum Stop {
        A(new StopFacilityInfo("A", new Coordinate(0, 0))),
        B(new StopFacilityInfo("B", new Coordinate(1, 1))),
        C(new StopFacilityInfo("C", new Coordinate(2, 2))),
        D(new StopFacilityInfo("D", new Coordinate(3, 3)));

        private final StopFacilityInfo stopFacilityInfo;
    }

    @RequiredArgsConstructor
    @Getter
    enum Segments {
        // A -- B
        A_B(List.of(new TrackSegmentInfo("segment_A-B", new Coordinate(0, 0), new Coordinate(1, 1), 1000))),
        B_A(List.of(new TrackSegmentInfo("segment_B-A", new Coordinate(1, 1), new Coordinate(0, 0), 1000))),

        // B -- C
        B_C(List.of(new TrackSegmentInfo("segment_B-C1", new Coordinate(1, 1), new Coordinate(1.5, 1.5), 500),
                new TrackSegmentInfo("segment_B-C2", new Coordinate(1.5, 1.5), new Coordinate(2, 2), 500))),
        C_B(List.of(new TrackSegmentInfo("segment_C-B1", new Coordinate(1.5, 1.5), new Coordinate(1, 1), 500),
                new TrackSegmentInfo("segment_C-B2", new Coordinate(2, 2), new Coordinate(1.5, 1.5), 500))),

        // C -- D
        C_D(List.of(new TrackSegmentInfo("segment_C-D1", new Coordinate(2, 2), new Coordinate(2.3, 2.3), 300),
                new TrackSegmentInfo("segment_C-D2", new Coordinate(2.3, 2.3), new Coordinate(2.7, 2.7), 400),
                new TrackSegmentInfo("segment_C-D3", new Coordinate(2.7, 2.7), new Coordinate(3, 3), 300))),
        D_C(List.of(new TrackSegmentInfo("segment_D_C1", new Coordinate(2.3, 2.3), new Coordinate(2, 2), 300),
                new TrackSegmentInfo("segment_D_C2", new Coordinate(2.7, 2.7), new Coordinate(2.3, 2.3), 400),
                new TrackSegmentInfo("segment_D_C3", new Coordinate(3, 3), new Coordinate(2.7, 2.7), 300)));

        private final List<TrackSegmentInfo> segments;
    }

    static class Default {
        private static final Duration DWELL_TIME = Duration.ofSeconds(2 * 60);
        private static final Duration TRAVEL_TIME = Duration.ofSeconds(5 * 60);
        private static final LocalTime SERVICE_DAY_START = LocalTime.of(6, 0);
    }

}

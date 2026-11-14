package ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.DepartureInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopAccess;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackNodeInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackSegmentInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitLineInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitRouteInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransportMode;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleTypeInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.Coordinate;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.Vehicles;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatsimSupplyBuilderTest {

    @Mock
    private InfrastructureRepository infrastructureRepository;

    @Mock
    private VehicleCircuitsPlanner vehicleCircuitsPlanner;

    @Mock
    private RollingStockRepository rollingStockRepository;

    private MatsimSupplyBuilder matsimSupplyBuilder;

    private static List<VehicleAllocation> mockVehicleAllocations() {
        TransitLineInfo transitLineInfo = new TransitLineInfo("lineSimple", "A", TransportMode.RAIL);

        VehicleInfo vehicleInfo1 = new VehicleInfo("vehicle1", VehicleType.VT_1.getVehicleTypeInfo());
        VehicleInfo vehicleInfo2 = new VehicleInfo("vehicle2", VehicleType.VT_1.getVehicleTypeInfo());
        VehicleInfo vehicleInfo3 = new VehicleInfo("vehicle3", VehicleType.VT_2.getVehicleTypeInfo());
        VehicleInfo vehicleInfo4 = new VehicleInfo("vehicle4", VehicleType.VT_2.getVehicleTypeInfo());

        DepartureInfo departureInfo1 = new DepartureInfo(new TransitRouteInfo("lineSimple_F", transitLineInfo),
                Default.SERVICE_DAY_START.plus(0, ChronoUnit.MINUTES));
        DepartureInfo departureInfo2 = new DepartureInfo(new TransitRouteInfo("lineSimple_F", transitLineInfo),
                Default.SERVICE_DAY_START.plus(5, ChronoUnit.MINUTES));
        DepartureInfo departureInfo3 = new DepartureInfo(new TransitRouteInfo("lineSimple_F", transitLineInfo),
                Default.SERVICE_DAY_START.plus(10, ChronoUnit.MINUTES));
        DepartureInfo departureInfo4 = new DepartureInfo(new TransitRouteInfo("lineSimple_R", transitLineInfo),
                Default.SERVICE_DAY_START.plus(1, ChronoUnit.MINUTES));
        DepartureInfo departureInfo5 = new DepartureInfo(new TransitRouteInfo("lineSimple_R", transitLineInfo),
                Default.SERVICE_DAY_START.plus(6, ChronoUnit.MINUTES));
        DepartureInfo departureInfo6 = new DepartureInfo(new TransitRouteInfo("lineSimple_R", transitLineInfo),
                Default.SERVICE_DAY_START.plus(11, ChronoUnit.MINUTES));

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
        matsimSupplyBuilder = new MatsimSupplyBuilder(infrastructureRepository, rollingStockRepository,
                vehicleCircuitsPlanner, ValidationStrategy.WARN_ON_ISSUES);
    }

    /**
     * Helper method to mock track segments for both forward and reverse directions.
     */
    private void mockTrackSegments(Stop stopA, Stop stopB, Segments segments) {
        when(infrastructureRepository.getTrack(eq(stopA.getStopFacilityInfo()), eq(stopB.getStopFacilityInfo()),
                any(TransitRouteInfo.class))).thenReturn(segments.getSegments());
    }

    /**
     * A -- B -- (C) -- D
     */
    @Test
    void testBuild() {
        // mock infrastructure
        when(infrastructureRepository.getStopFacility(eq("A"), anyString(), anyDouble(), anyDouble())).thenReturn(
                Stop.A.getStopFacilityInfo());
        when(infrastructureRepository.getStopFacility(eq("B"), anyString(), anyDouble(), anyDouble())).thenReturn(
                Stop.B.getStopFacilityInfo());
        when(infrastructureRepository.getStopFacility(eq("C"), anyString(), anyDouble(), anyDouble())).thenReturn(
                Stop.C.getStopFacilityInfo());
        when(infrastructureRepository.getStopFacility(eq("D"), anyString(), anyDouble(), anyDouble())).thenReturn(
                Stop.D.getStopFacilityInfo());

        // mock track segments: forward
        mockTrackSegments(Stop.A, Stop.B, Segments.A_B);
        mockTrackSegments(Stop.B, Stop.C, Segments.B_C);
        mockTrackSegments(Stop.C, Stop.D, Segments.C_D);
        // reverse
        mockTrackSegments(Stop.D, Stop.C, Segments.D_C);
        mockTrackSegments(Stop.C, Stop.B, Segments.C_B);
        mockTrackSegments(Stop.B, Stop.A, Segments.B_A);

        // mock vehicle type in rolling stock repository
        when(rollingStockRepository.getVehicleType(any())).thenReturn(VehicleType.VT_1.getVehicleTypeInfo());

        // mock the plan method
        when(vehicleCircuitsPlanner.plan()).thenReturn(mockVehicleAllocations());

        // act
        Scenario scenario = matsimSupplyBuilder.addStopFacility("A", "Stop A", 0, 0)
                .addStopFacility("B", "Stop B", 1, 1)
                .addStopFacility("C", "Stop C", 2, 2)
                .addStopFacility("D", "Stop D", 3, 3)
                .addTransitLine("lineSimple", "vehicleType")
                .addTransitRoute("lineSimple_F", "lineSimple", "A", Default.DWELL_TIME, Default.ACCESS)
                .addRouteStop("lineSimple_F", "B", Default.TRAVEL_TIME, Default.DWELL_TIME, Default.ACCESS)
                .addRoutePass("lineSimple_F", "C")
                .addRouteStop("lineSimple_F", "D", Default.TRAVEL_TIME.plus(Default.TRAVEL_TIME), Default.DWELL_TIME,
                        Default.ACCESS)
                .addTransitRoute("lineSimple_R", "lineSimple", "D", Default.DWELL_TIME, Default.ACCESS)
                .addRouteStop("lineSimple_R", "C", Default.TRAVEL_TIME, Default.DWELL_TIME, Default.ACCESS)
                .addRoutePass("lineSimple_R", "B")
                .addRouteStop("lineSimple_R", "A", Default.TRAVEL_TIME.plus(Default.TRAVEL_TIME), Default.DWELL_TIME,
                        Default.ACCESS)
                .addDeparture("lineSimple_F", Default.SERVICE_DAY_START.plus(0, ChronoUnit.MINUTES))
                .addDeparture("lineSimple_F", Default.SERVICE_DAY_START.plus(5, ChronoUnit.MINUTES))
                .addDeparture("lineSimple_F", Default.SERVICE_DAY_START.plus(10, ChronoUnit.MINUTES))
                .addDeparture("lineSimple_R", Default.SERVICE_DAY_START.plus(1, ChronoUnit.MINUTES))
                .addDeparture("lineSimple_R", Default.SERVICE_DAY_START.plus(6, ChronoUnit.MINUTES))
                .addDeparture("lineSimple_R", Default.SERVICE_DAY_START.plus(11, ChronoUnit.MINUTES))
                .build();

        // assert: 4 stops and 3 junctions, which both directions share; 4 stop links and 6 track links per direction
        Network network = scenario.getNetwork();
        assertNotNull(network);
        assertEquals(7, network.getNodes().size(), "Expected 7 nodes in the network");
        assertEquals(16, network.getLinks().size(), "Expected 16 links in the network");

        TransitSchedule schedule = scenario.getTransitSchedule();
        assertNotNull(schedule);
        TransitScheduleValidator.ValidationResult routes = TransitScheduleValidator.validateNetworkRoutes(schedule,
                network);
        assertTrue(routes.isValid(), () -> "Expected continuous network routes, but: " + routes.getErrors());
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
        A(new StopFacilityInfo("A", "Stop A", new Coordinate(0, 0))),
        B(new StopFacilityInfo("B", "Stop B", new Coordinate(1, 1))),
        C(new StopFacilityInfo("C", "Stop C", new Coordinate(2, 2))),
        D(new StopFacilityInfo("D", "Stop D", new Coordinate(3, 3)));

        private final StopFacilityInfo stopFacilityInfo;
    }

    @RequiredArgsConstructor
    @Getter
    enum TrackNode {
        A(new TrackNodeInfo("A", new Coordinate(0, 0))),
        B(new TrackNodeInfo("B", new Coordinate(1, 1))),
        C(new TrackNodeInfo("C", new Coordinate(2, 2))),
        D(new TrackNodeInfo("D", new Coordinate(3, 3))),
        // junctions between the stops
        BC(new TrackNodeInfo("junction_B-C", new Coordinate(1.5, 1.5))),
        CD1(new TrackNodeInfo("junction_C-D1", new Coordinate(2.3, 2.3))),
        CD2(new TrackNodeInfo("junction_C-D2", new Coordinate(2.7, 2.7)));

        private final TrackNodeInfo trackNodeInfo;
    }

    @RequiredArgsConstructor
    @Getter
    enum Segments {
        // A -- B
        A_B(List.of(segment("segment_A-B", TrackNode.A, TrackNode.B, 1000))),
        B_A(List.of(segment("segment_B-A", TrackNode.B, TrackNode.A, 1000))),

        // B -- C, over one junction
        B_C(List.of(segment("segment_B-C1", TrackNode.B, TrackNode.BC, 500),
                segment("segment_B-C2", TrackNode.BC, TrackNode.C, 500))),
        C_B(List.of(segment("segment_C-B1", TrackNode.C, TrackNode.BC, 500),
                segment("segment_C-B2", TrackNode.BC, TrackNode.B, 500))),

        // C -- D, over two junctions
        C_D(List.of(segment("segment_C-D1", TrackNode.C, TrackNode.CD1, 300),
                segment("segment_C-D2", TrackNode.CD1, TrackNode.CD2, 400),
                segment("segment_C-D3", TrackNode.CD2, TrackNode.D, 300))),
        D_C(List.of(segment("segment_D-C1", TrackNode.D, TrackNode.CD2, 300),
                segment("segment_D-C2", TrackNode.CD2, TrackNode.CD1, 400),
                segment("segment_D-C3", TrackNode.CD1, TrackNode.C, 300)));

        private final List<TrackSegmentInfo> segments;

        private static TrackSegmentInfo segment(String id, TrackNode from, TrackNode to, double length) {
            return new TrackSegmentInfo(id, from.getTrackNodeInfo(), to.getTrackNodeInfo(), length);
        }
    }

    @RequiredArgsConstructor
    @Getter
    enum VehicleType {
        VT_1(new VehicleTypeInfo("vehicleType1", TransportMode.RAIL, 100, 50, 200.0, 50.0, Map.of())),
        VT_2(new VehicleTypeInfo("vehicleType2", TransportMode.RAIL, 150, 0, 300.0, 60.0, Map.of()));

        private final VehicleTypeInfo vehicleTypeInfo;
    }

    static class Default {
        private static final Duration DWELL_TIME = Duration.ofSeconds(2 * 60);
        private static final Duration TRAVEL_TIME = Duration.ofSeconds(5 * 60);
        private static final ServiceDayTime SERVICE_DAY_START = ServiceDayTime.of(6, 0, 0);
        private static final StopAccess ACCESS = StopAccess.BOARDING_AND_ALIGHTING;
    }
}

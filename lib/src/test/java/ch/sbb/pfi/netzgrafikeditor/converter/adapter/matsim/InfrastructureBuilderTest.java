package ch.sbb.pfi.netzgrafikeditor.converter.adapter.matsim;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteElement;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteStop;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackNodeInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackSegmentInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitLineInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitRouteInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransportMode;
import ch.sbb.pfi.netzgrafikeditor.converter.core.validation.ValidationStrategy;
import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.Coordinate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.utils.TransitScheduleValidator;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Builds the network of transit routes over tracks made of several segments.
 * <p>
 * The stops A, B, D and E lie around the junctions J and K:
 * <pre>
 *     B           E
 *       \       /
 *        J --- K
 *       /       \
 *     A           D
 * </pre>
 */
class InfrastructureBuilderTest {

    private static final TransitLineInfo LINE = new TransitLineInfo("line", "category", TransportMode.RAIL);

    private final Map<String, TrackNodeInfo> nodes = new HashMap<>();
    private final Map<String, StopFacilityInfo> stops = new HashMap<>();
    private final Map<String, List<TrackSegmentInfo>> tracks = new HashMap<>();

    private final InfrastructureRepository repository = new InfrastructureRepository() {

        @Override
        public StopFacilityInfo getStopFacility(String stopId, String stopName, double x, double y) {
            return stops.get(stopId);
        }

        @Override
        public List<TrackSegmentInfo> getTrack(StopFacilityInfo fromStop, StopFacilityInfo toStop, TransitRouteInfo transitRouteInfo) {
            return tracks.getOrDefault(fromStop.getId() + ">" + toStop.getId(), List.of());
        }

    };

    private Scenario scenario;

    private static void assertHandledAccordingTo(ValidationStrategy strategy, InfrastructureBuilder builder) {
        if (strategy == ValidationStrategy.FAIL_ON_ISSUES) {
            assertThrows(IllegalStateException.class, builder::handleTrackIssues);
        } else {
            assertDoesNotThrow(builder::handleTrackIssues);
        }
    }

    @BeforeEach
    void setUp() {
        scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        stop("A", 0, 0);
        stop("B", 2, 0);
        stop("D", 0, 4);
        stop("E", 2, 4);
        junction("J", 1, 1);
        junction("K", 1, 3);
    }

    private void stop(String id, double latitude, double longitude) {
        Coordinate coordinate = new Coordinate(latitude, longitude);
        stops.put(id, new StopFacilityInfo(id, "Stop " + id, coordinate));
        nodes.put(id, new TrackNodeInfo(id, coordinate));
    }

    private void junction(String id, double latitude, double longitude) {
        nodes.put(id, new TrackNodeInfo(id, new Coordinate(latitude, longitude)));
    }

    private void track(String fromStopId, String toStopId, TrackSegmentInfo... segments) {
        tracks.put(fromStopId + ">" + toStopId, List.of(segments));
    }

    private TrackSegmentInfo segment(String id, String fromNodeId, String toNodeId) {
        return new TrackSegmentInfo(id, nodes.get(fromNodeId), nodes.get(toNodeId), 1000);
    }

    /**
     * Builds the stops and one transit route per route, each given as its stops joined by '>', e.g. "A>D".
     */
    private InfrastructureBuilder build(ValidationStrategy strategy, String... routes) {
        InfrastructureBuilder builder = new InfrastructureBuilder(new MatsimSupplyFactory(scenario), repository,
                strategy);
        stops.values().forEach(builder::buildTransitStopFacility);
        for (String route : routes) {
            List<RouteElement> routeElements = Arrays.stream(route.split(">"))
                    .map(stopId -> (RouteElement) new RouteStop(stops.get(stopId), Duration.ZERO, Duration.ZERO))
                    .toList();
            builder.buildTransitRoute(new TransitRouteInfo(route, LINE), routeElements);
        }
        return builder;
    }

    private boolean routesAreContinuous() {
        return TransitScheduleValidator.validateNetworkRoutes(scenario.getTransitSchedule(), scenario.getNetwork())
                .isValid();
    }

    private void assertRoutesAreContinuous() {
        TransitScheduleValidator.ValidationResult result = TransitScheduleValidator.validateNetworkRoutes(
                scenario.getTransitSchedule(), scenario.getNetwork());
        assertTrue(result.isValid(), () -> "Expected continuous network routes, but: " + result.getErrors());
    }

    @Nested
    class Junctions {

        @Test
        void consecutiveSegmentsShareTheirJunction() {
            track("A", "D", segment("A-J", "A", "J"), segment("J-D", "J", "D"));

            build(ValidationStrategy.FAIL_ON_ISSUES, "A>D").handleTrackIssues();

            assertRoutesAreContinuous();
            assertEquals(5, scenario.getNetwork().getNodes().size(), "Expected the 4 stops and junction J");
            assertTrue(scenario.getNetwork().getLinks().containsKey(Id.createLinkId("track_link_A-J")));
            assertTrue(scenario.getNetwork().getLinks().containsKey(Id.createLinkId("track_link_J-D")));
        }

        @Test
        void routesConvergingOnASegmentMeetAtItsJunction() {
            track("A", "D", segment("A-J", "A", "J"), segment("J-D", "J", "D"));
            track("B", "D", segment("B-J", "B", "J"), segment("J-D", "J", "D"));

            build(ValidationStrategy.FAIL_ON_ISSUES, "A>D", "B>D").handleTrackIssues();

            assertRoutesAreContinuous();
            assertEquals(5, scenario.getNetwork().getNodes().size(), "Expected the 4 stops and junction J");
        }

        @Test
        void routeOverSegmentsOfTwoOtherRoutesIsContinuous() {
            // A>D and B>E add their segments first, each meeting the other at J; A>E then reuses one of each
            track("A", "D", segment("A-J", "A", "J"), segment("J-D", "J", "D"));
            track("B", "E", segment("B-J", "B", "J"), segment("J-E", "J", "E"));
            track("A", "E", segment("A-J", "A", "J"), segment("J-E", "J", "E"));

            build(ValidationStrategy.FAIL_ON_ISSUES, "A>D", "B>E", "A>E").handleTrackIssues();

            assertRoutesAreContinuous();
            assertEquals(5, scenario.getNetwork().getNodes().size(), "Expected the 4 stops and junction J");
            assertEquals(8, scenario.getNetwork().getLinks().size(), "Expected 4 stop links and 4 track links");
        }

        @Test
        void parallelSegmentsBetweenTwoJunctionsStayDistinctLinks() {
            track("A", "D", segment("A-J", "A", "J"), segment("J-K-north", "J", "K"), segment("K-D", "K", "D"));
            track("B", "E", segment("B-J", "B", "J"), segment("J-K-south", "J", "K"), segment("K-E", "K", "E"));

            build(ValidationStrategy.FAIL_ON_ISSUES, "A>D", "B>E").handleTrackIssues();

            assertRoutesAreContinuous();
            assertEquals(6, scenario.getNetwork().getNodes().size(), "Expected the 4 stops and junctions J and K");
            Map<Id<Link>, ? extends Link> links = scenario.getNetwork().getLinks();
            assertTrue(links.containsKey(Id.createLinkId("track_link_J-K-north")));
            assertTrue(links.containsKey(Id.createLinkId("track_link_J-K-south")));
        }

    }

    @Nested
    class BrokenTracks {

        @ParameterizedTest
        @EnumSource(ValidationStrategy.class)
        void segmentsThatDoNotMeet(ValidationStrategy strategy) {
            track("A", "D", segment("A-J", "A", "J"), segment("K-D", "K", "D"));

            assertHandledAccordingTo(strategy, build(strategy, "A>D"));
        }

        @ParameterizedTest
        @EnumSource(ValidationStrategy.class)
        void trackThatDoesNotStartAtItsStop(ValidationStrategy strategy) {
            track("A", "D", segment("J-D", "J", "D"));

            assertHandledAccordingTo(strategy, build(strategy, "A>D"));
        }

        @ParameterizedTest
        @EnumSource(ValidationStrategy.class)
        void trackThatDoesNotEndAtItsStop(ValidationStrategy strategy) {
            track("A", "D", segment("A-J", "A", "J"));

            assertHandledAccordingTo(strategy, build(strategy, "A>D"));
        }

        @ParameterizedTest
        @EnumSource(ValidationStrategy.class)
        void trackWithoutSegments(ValidationStrategy strategy) {
            track("A", "D");

            assertHandledAccordingTo(strategy, build(strategy, "A>D"));
        }

        @ParameterizedTest
        @EnumSource(ValidationStrategy.class)
        void segmentReusedBetweenOtherNodes(ValidationStrategy strategy) {
            // the reverse track reuses the id of segment A-J for the opposite direction
            track("A", "D", segment("A-J", "A", "J"), segment("J-D", "J", "D"));
            track("D", "A", segment("D-J", "D", "J"), segment("A-J", "J", "A"));

            assertHandledAccordingTo(strategy, build(strategy, "A>D", "D>A"));
        }

        @ParameterizedTest
        @EnumSource(ValidationStrategy.class)
        void nodeReusedAtAnotherCoordinate(ValidationStrategy strategy) {
            track("A", "D", segment("A-J", "A", "J"), segment("J-D", "J", "D"));
            TrackNodeInfo movedJunction = new TrackNodeInfo("J", new Coordinate(1.5, 1));
            track("B", "D", new TrackSegmentInfo("B-J", nodes.get("B"), movedJunction, 1000),
                    new TrackSegmentInfo("J-D", movedJunction, nodes.get("D"), 1000));

            assertHandledAccordingTo(strategy, build(strategy, "A>D", "B>D"));
        }

        @Test
        void brokenTrackIsBuiltAsGivenWhenWarning() {
            track("A", "D", segment("A-J", "A", "J"), segment("K-D", "K", "D"));

            build(ValidationStrategy.WARN_ON_ISSUES, "A>D").handleTrackIssues();

            assertFalse(routesAreContinuous(), "Expected the route to keep the gap the repository gave");
        }

    }

}

package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Agency;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Calendar;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Route;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.RouteType;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Stop;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.StopTime;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Trip;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.BaseSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RollingStockRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteElement;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteElementVisitor;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RoutePass;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteStop;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransportMode;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;
import ch.sbb.pfi.netzgrafikeditor.converter.util.time.ServiceDayTime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GtfsSupplyBuilder extends BaseSupplyBuilder<GtfsSchedule> {

    public static final String ROUTE_NAME_FORMAT = "%s: %s - %s";

    private final List<Stop> stops = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final List<Trip> trips = new ArrayList<>();
    private final List<StopTime> stopTimes = new ArrayList<>();

    private final Set<String> createdRoutes = new HashSet<>();
    private final Map<String, Integer> tripCounts = new HashMap<>();
    private final Map<String, List<RouteElement>> routeElements = new HashMap<>();

    public GtfsSupplyBuilder(InfrastructureRepository infrastructureRepository, RollingStockRepository rollingStockRepository, VehicleCircuitsPlanner vehicleCircuitsPlanner) {
        super(infrastructureRepository, rollingStockRepository, vehicleCircuitsPlanner);
    }

    private static String getNameOrIdIfNull(StopFacilityInfo stopFacilityInfo) {
        return stopFacilityInfo.getName().isEmpty() ? stopFacilityInfo.getId() : stopFacilityInfo.getName();
    }

    private static RouteType toGtfsRouteType(TransportMode transportMode) {
        if (transportMode == null) {
            throw new IllegalArgumentException("Input TransportMode cannot be null.");
        }

        return switch (transportMode) {
            case TRAM -> RouteType.TRAM;
            case SUBWAY -> RouteType.SUBWAY;
            case RAIL -> RouteType.RAIL;
            case BUS -> RouteType.BUS;
            case FERRY -> RouteType.FERRY;
            case CABLE_TRAM -> RouteType.CABLE_TRAM;
            case AERIAL_LIFT -> RouteType.AERIAL_LIFT;
            case FUNICULAR -> RouteType.FUNICULAR;
            case TROLLEYBUS -> RouteType.TROLLEYBUS;
            case MONORAIL -> RouteType.MONORAIL;
        };
    }

    @Override
    protected void buildStopFacility(StopFacilityInfo stopFacilityInfo) {
        stops.add(Stop.builder()
                .stopId(stopFacilityInfo.getId())
                .stopName(stopFacilityInfo.getName())
                .stopLat(stopFacilityInfo.getCoordinate().getLatitude())
                .stopLon(stopFacilityInfo.getCoordinate().getLongitude())
                .build());
    }

    @Override
    protected void buildDeparture(VehicleAllocation vehicleAllocation) {

        String routeId = vehicleAllocation.getDepartureInfo().getTransitRouteInfo().getId();
        String tripId = String.format("%s_%d", routeId, tripCounts.merge(routeId, 1, Integer::sum));
        List<RouteElement> currentRouteElements = routeElements.get(routeId);

        // create and add trip
        trips.add(Trip.builder()
                .routeId(vehicleAllocation.getDepartureInfo().getTransitRouteInfo().getTransitLineInfo().getId())
                .serviceId(Calendar.DEFAULT_ID)
                .tripId(tripId)
                .tripHeadsign(currentRouteElements.getLast().getStopFacilityInfo().getId())
                .build());

        // create and add stop times: gtfs stop time sequence starts with 1 not 0
        final int[] count = {1};
        final ServiceDayTime[] time = {vehicleAllocation.getDepartureInfo().getTime()};

        for (RouteElement routeElement : currentRouteElements) {
            routeElement.accept(new RouteElementVisitor() {

                @Override
                public void visit(RouteStop routeStop) {
                    Duration travelTime = routeStop.getTravelTime();
                    Duration dwellTime = routeStop.getDwellTime();

                    // set time to arrival time if at start of stop time sequence
                    if (count[0] == 1) {
                        time[0] = time[0].minus(dwellTime);
                    }

                    time[0] = time[0].plus(travelTime);
                    ServiceDayTime arrivalTime = time[0];
                    time[0] = time[0].plus(dwellTime);
                    ServiceDayTime departureTime = time[0];

                    stopTimes.add(StopTime.builder()
                            .tripId(tripId)
                            .arrivalTime(arrivalTime)
                            .departureTime(departureTime)
                            .stopId(routeStop.getStopFacilityInfo().getId())
                            .stopSequence(count[0]++)
                            .build());
                }

                @Override
                public void visit(RoutePass routePass) {
                    // nothing to do
                }
            });

        }

    }

    @Override
    protected GtfsSchedule getResult() {
        return GtfsSchedule.builder().stops(stops).routes(routes).trips(trips).stopTimes(stopTimes).build();
    }

    @Override
    protected void buildTransitRoute(TransitRouteContainer transitRouteContainer) {

        // store route elements for stop time creation
        routeElements.put(transitRouteContainer.transitRouteInfo().getId(), transitRouteContainer.routeElements());

        // build transit route names
        String categoryName = transitRouteContainer.transitRouteInfo().getTransitLineInfo().getCategory();
        StopFacilityInfo orig = transitRouteContainer.routeElements().getFirst().getStopFacilityInfo();
        StopFacilityInfo dest = transitRouteContainer.routeElements().getLast().getStopFacilityInfo();
        String routeShortName = String.format(ROUTE_NAME_FORMAT, categoryName, orig.getId(), dest.getId());
        String routeLongName = String.format(ROUTE_NAME_FORMAT, categoryName, getNameOrIdIfNull(orig),
                getNameOrIdIfNull(dest));

        // create and add GTFS route (transit line in the context of the supply builder) if not yet added
        String routeId = transitRouteContainer.transitRouteInfo().getTransitLineInfo().getId();
        if (!createdRoutes.contains(routeId)) {
            routes.add(Route.builder()
                    .routeId(routeId)
                    .agencyId(Agency.DEFAULT_ID)
                    .routeLongName(routeLongName)
                    .routeShortName(routeShortName)
                    .routeType(toGtfsRouteType(
                            transitRouteContainer.transitRouteInfo().getTransitLineInfo().getTransportMode()))
                    .build());
            createdRoutes.add(routeId);
        }
    }
}

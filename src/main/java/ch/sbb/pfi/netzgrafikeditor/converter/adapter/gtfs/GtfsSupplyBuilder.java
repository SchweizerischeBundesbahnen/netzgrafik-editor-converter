package ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Agency;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Calendar;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Route;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Stop;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.StopTime;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Trip;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.BaseSupplyBuilder;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteElement;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteElementVisitor;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RoutePass;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.RouteStop;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleAllocation;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.VehicleCircuitsPlanner;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GtfsSupplyBuilder extends BaseSupplyBuilder<GtfsSchedule> {

    public static final int ROUTE_TYPE = 0;
    private static final Agency AGENCY = Agency.builder()
            .agencyId("nge")
            .agencyName("Netzgrafik Editor")
            .agencyTimeZone("UTC")
            .agencyUrl("https://github.com/SchweizerischeBundesbahnen/netzgrafik-editor-frontend")
            .build();
    private static final Calendar CALENDAR = Calendar.builder()
            .serviceId("always")
            .monday(Calendar.Type.AVAILABLE)
            .tuesday(Calendar.Type.AVAILABLE)
            .wednesday(Calendar.Type.AVAILABLE)
            .thursday(Calendar.Type.AVAILABLE)
            .friday(Calendar.Type.AVAILABLE)
            .startDate(LocalDate.MAX)
            .endDate(LocalDate.MAX)
            .build();
    private final List<Stop> stops = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final List<Trip> trips = new ArrayList<>();
    private final List<StopTime> stopTimes = new ArrayList<>();

    private final Set<String> createdRoutes = new HashSet<>();
    private final Map<String, List<RouteElement>> routeElements = new HashMap<>();

    public GtfsSupplyBuilder(InfrastructureRepository infrastructureRepository, VehicleCircuitsPlanner vehicleCircuitsPlanner) {
        super(infrastructureRepository, vehicleCircuitsPlanner);
    }

    @Override
    protected void buildStopFacility(StopFacilityInfo stopFacilityInfo) {
        stops.add(Stop.builder()
                .stopId(stopFacilityInfo.getId())
                .stopName(stopFacilityInfo.getId())
                .stopLat(stopFacilityInfo.getCoordinate().getLatitude())
                .stopLon(stopFacilityInfo.getCoordinate().getLongitude())
                .build());
    }

    @Override
    protected void buildTransitRoute(TransitRouteContainer transitRouteContainer) {

        // store route elements for stop time creation
        routeElements.put(transitRouteContainer.transitRouteInfo().getId(), transitRouteContainer.routeElements());

        // create and add GTFS route (transit line in the context of the supply builder) if not yet added
        String routeId = transitRouteContainer.transitRouteInfo().getTransitLineInfo().getId();
        if (!createdRoutes.contains(routeId)) {
            routes.add(Route.builder()
                    .routeId(routeId)
                    .agencyId(AGENCY.getAgencyId())
                    .routeLongName(routeId)
                    .routeShortName(routeId)
                    .routeType(ROUTE_TYPE)
                    .build());
            createdRoutes.add(routeId);
        }

        // create and add trip
        trips.add(Trip.builder()
                .routeId(routeId)
                .serviceId(CALENDAR.getServiceId())
                .tripId(transitRouteContainer.transitRouteInfo().getId())
                .tripHeadsign(transitRouteContainer.routeElements().getLast().getStopFacilityInfo().getId())
                .build());
    }

    @Override
    protected void buildDeparture(VehicleAllocation vehicleAllocation) {

        String tripId = vehicleAllocation.getDepartureInfo().getTransitRouteInfo().getId();
        final LocalTime[] time = {vehicleAllocation.getDepartureInfo().getTime()};
        final int[] count = {0};

        for (RouteElement routeElement : routeElements.get(tripId)) {
            routeElement.accept(new RouteElementVisitor() {

                @Override
                public void visit(RouteStop routeStop) {
                    Duration travelTime = routeStop.getTravelTime();
                    Duration dwellTime = routeStop.getDwellTime();

                    // set time to arrival time if at start of stop time sequence
                    if (count[0] == 0) {
                        time[0] = time[0].minus(dwellTime);
                    }

                    time[0] = time[0].plus(travelTime);
                    LocalTime arrivalTime = time[0];
                    time[0] = time[0].plus(dwellTime);
                    LocalTime departureTime = time[0];

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
        return GtfsSchedule.builder()
                .agencies(List.of(AGENCY))
                .stops(stops)
                .routes(routes)
                .trips(trips)
                .stopTimes(stopTimes)
                .calendars(List.of(CALENDAR))
                .build();
    }
}

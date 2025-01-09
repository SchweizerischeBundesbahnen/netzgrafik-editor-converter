package ch.sbb.pfi.netzgrafikeditor.converter.core.supply.fallback;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackSegmentInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitRouteInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.Coordinate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoInfrastructureRepository implements InfrastructureRepository {

    private final Map<String, Coordinate> coordinates = new HashMap<>();

    @Override
    public StopFacilityInfo getStopFacility(String stopId, String stopName, double x, double y) {
        Coordinate coordinate = new Coordinate(-y, x);
        coordinates.put(stopId, coordinate);

        return new StopFacilityInfo(stopId, stopName, coordinate);
    }

    @Override
    public List<TrackSegmentInfo> getTrack(StopFacilityInfo fromStop, StopFacilityInfo toStop, TransitRouteInfo transitRouteInfo) {
        Coordinate fromCoord = coordinates.get(fromStop.getId());
        Coordinate toCoord = coordinates.get(toStop.getId());

        double distance = euclideanDistance(fromCoord, toCoord);

        return List.of(
                new TrackSegmentInfo(String.format("%s-%s", fromStop.getId(), toStop.getId()), fromCoord, toCoord,
                        distance));
    }

    private double euclideanDistance(Coordinate from, Coordinate to) {
        // coordinates represent screen positions (not geographical); Euclidean formula is valid in this case
        double deltaX = to.getLongitude() - from.getLongitude();
        double deltaY = to.getLatitude() - from.getLatitude();

        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}

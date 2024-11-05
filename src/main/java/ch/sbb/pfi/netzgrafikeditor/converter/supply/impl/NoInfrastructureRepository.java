package ch.sbb.pfi.netzgrafikeditor.converter.supply.impl;

import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;
import ch.sbb.pfi.netzgrafikeditor.converter.model.Node;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.Coordinate;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.TrackSegmentInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.supply.TransitLineInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NoInfrastructureRepository implements InfrastructureRepository {

    private final Map<String, Node> nodes = new HashMap<>();

    public NoInfrastructureRepository(NetworkGraphic networkGraphic) {
        networkGraphic.getNodes().forEach(node -> nodes.put(node.getBetriebspunktName(), node));
    }

    @Override
    public StopFacilityInfo getStopFacility(String stopId) {
        Node node = nodes.get(stopId);
        return new StopFacilityInfo(stopId, new Coordinate(-node.getPositionY(), node.getPositionX()));
    }

    @Override
    public List<TrackSegmentInfo> getTrack(StopFacilityInfo fromStop, StopFacilityInfo toStop, TransitLineInfo transitLineInfo) {
        Node fromNode = nodes.get(fromStop.getId());
        Node toNode = nodes.get(toStop.getId());

        Coordinate fromCoord = new Coordinate(fromNode.getPositionX(), fromNode.getPositionY());
        Coordinate toCoord = new Coordinate(toNode.getPositionX(), toNode.getPositionY());
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

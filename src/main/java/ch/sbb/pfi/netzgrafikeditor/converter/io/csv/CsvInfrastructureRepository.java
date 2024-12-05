package ch.sbb.pfi.netzgrafikeditor.converter.io.csv;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackSegmentInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitRouteInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.Coordinate;
import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.HaversineDistance;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class CsvInfrastructureRepository extends CsvRepository<StopFacilityInfo> implements InfrastructureRepository {

    public CsvInfrastructureRepository(Path filePath) throws IOException {
        super(filePath, record -> {
            String stopId = record.get("stop_id");
            String stopName = record.get("stop_name");
            double stopLat = Double.parseDouble(record.get("stop_lat"));
            double stopLon = Double.parseDouble(record.get("stop_lon"));

            return new Entry<>(stopId, new StopFacilityInfo(stopId, stopName, new Coordinate(stopLat, stopLon)));
        });
    }

    @Override
    public StopFacilityInfo getStopFacility(String stopId, String stopName, double x, double y) {
        return getEntity(stopId);
    }

    @Override
    public List<TrackSegmentInfo> getTrack(StopFacilityInfo fromStop, StopFacilityInfo toStop, TransitRouteInfo transitRouteInfo) {
        Coordinate fromCoord = getEntity(fromStop.getId()).getCoordinate();
        Coordinate toCoord = getEntity(toStop.getId()).getCoordinate();

        return List.of(
                new TrackSegmentInfo(String.format("%s-%s", fromStop.getId(), toStop.getId()), fromCoord, toCoord,
                        HaversineDistance.calculate(fromCoord, toCoord)));
    }
}

package ch.sbb.pfi.netzgrafikeditor.converter.io.csv;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.InfrastructureRepository;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackSegmentInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TransitRouteInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.Coordinate;
import ch.sbb.pfi.netzgrafikeditor.converter.util.spatial.HaversineDistance;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
public class CsvInfrastructureRepository implements InfrastructureRepository {

    private final Map<String, StopFacilityInfo> stopFacilityInfos = new HashMap<>();

    // TODO: Remove deprecated BOMInputStream, robust handling of encoding
    public CsvInfrastructureRepository(Path filePath) throws IOException {
        log.info("Reading stop facility info CSV file {}", filePath);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new BOMInputStream(Files.newInputStream(filePath)), StandardCharsets.UTF_8));
        CSVParser csvParser = new CSVParser(reader,
                CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build());
        {
            for (CSVRecord record : csvParser) {
                String stopId = record.get("stop_id");
                // String stopName = record.get("stop_name");
                double stopLat = Double.parseDouble(record.get("stop_lat"));
                double stopLon = Double.parseDouble(record.get("stop_lon"));

                stopFacilityInfos.put(stopId, new StopFacilityInfo(stopId, new Coordinate(stopLat, stopLon)));
            }
        }
    }

    @Override
    public StopFacilityInfo getStopFacility(String stopId, double x, double y) {
        return getStopFacility(stopId);
    }

    private StopFacilityInfo getStopFacility(String stopId) {
        StopFacilityInfo stopFacilityInfo = stopFacilityInfos.get(stopId);
        if (stopFacilityInfo == null) {
            throw new IllegalArgumentException("No stop facility entry not found in CSV for " + stopId);
        }

        return stopFacilityInfo;
    }

    @Override
    public List<TrackSegmentInfo> getTrack(StopFacilityInfo fromStop, StopFacilityInfo toStop, TransitRouteInfo transitRouteInfo) {
        Coordinate fromCoord = getStopFacility(fromStop.getId()).getCoordinate();
        Coordinate toCoord = getStopFacility(toStop.getId()).getCoordinate();

        return List.of(
                new TrackSegmentInfo(String.format("%s-%s", fromStop.getId(), toStop.getId()), fromCoord, toCoord,
                        HaversineDistance.calculate(fromCoord, toCoord)));
    }
}

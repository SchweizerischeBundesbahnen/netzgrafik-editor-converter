package ch.sbb.pfi.netzgrafikeditor.converter.io.csv;

import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.StopFacilityInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.core.supply.TrackSegmentInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.util.test.TestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvInfrastructureRepositoryTest {

    private CsvInfrastructureRepository repo;

    @BeforeEach
    void setUp() throws IOException {
        repo = new CsvInfrastructureRepository(TestScenario.ALL_TEST_CASES.getStopFacilityInfoCsvFilePath());
    }

    @Test
    void getStopFacility() {
        StopFacilityInfo stopFacilityInfo = repo.getStopFacility("A", "Stop A", 0., 0.);
        assertEquals("A", stopFacilityInfo.getId());
        assertEquals(46.948, stopFacilityInfo.getCoordinate().getLatitude());
        assertEquals(7.4474, stopFacilityInfo.getCoordinate().getLongitude());
    }

    @Test
    void getTrack() {
        List<TrackSegmentInfo> trackSegmentInfos = repo.getTrack(new StopFacilityInfo("A", "Stop A", null),
                new StopFacilityInfo("B", "Stop B", null), null);
        assertEquals(1, trackSegmentInfos.size());
        assertEquals(655.3, trackSegmentInfos.get(0).getLength(), 0.1);
    }
}
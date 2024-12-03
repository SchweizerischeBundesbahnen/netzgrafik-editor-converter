package ch.sbb.pfi.netzgrafikeditor.converter.io.gtfs;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Agency;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Calendar;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Route;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Stop;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.StopTime;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Trip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GtfsScheduleWriterTest {

    private static final GtfsSchedule SCHEDULE = GtfsSchedule.builder()
            .agencies(List.of(Agency.builder().build()))
            .stops(List.of(Stop.builder().build()))
            .routes(List.of(Route.builder().build()))
            .trips(List.of(Trip.builder().build()))
            .stopTimes(List.of(StopTime.builder().build()))
            .calendars(List.of(Calendar.builder().build()))
            .build();

    private static final GtfsSchedule SCHEDULE_EMPTY = GtfsSchedule.builder().build();

    private GtfsScheduleWriter writer;
    private Path outputDir;

    @Nested
    class Csv {

        @BeforeEach
        void setUp(@TempDir Path tempDir) {
            outputDir = tempDir;
            writer = new GtfsScheduleWriter(outputDir, false);
        }

        @Test
        void save() throws IOException {
            writer.save(SCHEDULE);
            ensureFiles();
        }

        @Test
        void save_empty() throws IOException {
            writer.save(SCHEDULE_EMPTY);
            ensureFiles();
        }

        private void ensureFiles() {
            for (GtfsScheduleWriter.GtfsFile file : GtfsScheduleWriter.GtfsFile.values()) {
                assertTrue(outputDir.resolve(file.getFileName()).toFile().exists());
            }
        }
    }

    @Nested
    class Zip {

        @BeforeEach
        void setUp(@TempDir Path tempDir) {
            outputDir = tempDir;
            writer = new GtfsScheduleWriter(outputDir, true);
        }

        @Test
        void save() throws IOException {
            writer.save(SCHEDULE);
            ensureZip();
        }

        @Test
        void save_empty() throws IOException {
            writer.save(SCHEDULE_EMPTY);
            ensureZip();
        }

        private void ensureZip() {
            assertTrue(outputDir.resolve(GtfsScheduleWriter.GTFS_ZIP).toFile().exists());
        }
    }
}

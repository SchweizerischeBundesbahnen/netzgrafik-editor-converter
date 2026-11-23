package ch.sbb.pfi.netzgrafikeditor.converter.io.gtfs;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Agency;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Calendar;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Route;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Stop;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.StopTime;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Transfer;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.TransferType;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Trip;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtfsScheduleWriterTest {

    private static final GtfsSchedule SCHEDULE = GtfsSchedule.builder()
            .agencies(List.of(Agency.builder().build()))
            .stops(List.of(Stop.builder().build()))
            .routes(List.of(Route.builder().build()))
            .trips(List.of(Trip.builder().build()))
            .stopTimes(List.of(StopTime.builder().build()))
            .calendars(List.of(Calendar.builder().build()))
            .transfers(List.of(Transfer.builder()
                    .fromStopId("a")
                    .toStopId("a")
                    .fromTripId("trip1")
                    .toTripId("trip2")
                    .transferType(TransferType.TIMED)
                    .build(), Transfer.builder()
                    .fromStopId("b")
                    .toStopId("b")
                    .transferType(TransferType.NOT_POSSIBLE)
                    .build()))
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
            ensureFiles(true);
        }

        @Test
        void save_empty() throws IOException {
            writer.save(SCHEDULE_EMPTY);
            ensureFiles(false);
        }

        @Test
        void save_transfers() throws IOException {
            writer.save(SCHEDULE);

            List<String> lines = Files.readAllLines(outputDir.resolve("transfers.txt"));
            assertEquals(List.of(
                    "from_stop_id,to_stop_id,from_route_id,to_route_id,from_trip_id,to_trip_id,transfer_type,min_transfer_time",
                    "a,a,,,trip1,trip2,1,", "b,b,,,,,3,"), lines);
        }

        @Test
        void save_stopTimeAccess() throws IOException {
            writer.save(GtfsSchedule.builder()
                    .stopTimes(List.of(StopTime.builder().tripId("trip1").stopId("a").stopSequence(1).build(),
                            StopTime.builder()
                                    .tripId("trip1")
                                    .stopId("b")
                                    .stopSequence(2)
                                    .pickupType(StopTime.NOT_AVAILABLE)
                                    .dropOffType(StopTime.NOT_AVAILABLE)
                                    .build()))
                    .build());

            List<String> lines = Files.readAllLines(outputDir.resolve("stop_times.txt"));
            assertEquals(List.of("trip_id,arrival_time,departure_time,stop_id,stop_sequence,pickup_type,drop_off_type",
                    "trip1,,,a,1,,", "trip1,,,b,2,1,1"), lines);
        }

        /**
         * The required files are always written, the optional ones only if they have rows.
         */
        private void ensureFiles(boolean withOptionalRows) {
            for (GtfsScheduleWriter.GtfsFile file : GtfsScheduleWriter.GtfsFile.values()) {
                boolean expected = !file.isOptional() || withOptionalRows;
                assertEquals(expected, outputDir.resolve(file.getFileName()).toFile().exists(), file.getFileName());
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

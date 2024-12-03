package ch.sbb.pfi.netzgrafikeditor.converter.io.gtfs;

import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Agency;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Calendar;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.FeedInfo;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.GtfsSchedule;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Route;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Stop;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.StopTime;
import ch.sbb.pfi.netzgrafikeditor.converter.adapter.gtfs.model.Trip;
import ch.sbb.pfi.netzgrafikeditor.converter.core.ConverterSink;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RequiredArgsConstructor
@Slf4j
public class GtfsScheduleWriter implements ConverterSink<GtfsSchedule> {

    public static final String GTFS_ZIP = "gtfs.zip";
    public static final String DELIMITER = ",";
    public static final String NA_VALUE = "";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Path directory;
    private final boolean zip;

    private static <T> void writeList(List<T> list, Class<?> clazz, OutputStream os) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        Field[] fields = clazz.getDeclaredFields();

        try {
            // write header
            String header = generateHeader(fields);
            writer.write(header);
            writer.newLine();

            // check for empty list
            if (list == null || list.isEmpty()) {
                log.warn("Writing empty CSV file: {}", clazz.getSimpleName());
                return;
            }

            // write data
            for (T item : list) {
                String data = generateDataLine(item, fields);
                writer.write(data);
                writer.newLine();
            }

        } finally {
            // ensure all data is written but do not close the writer since it would also close th underlying output
            // stream, which has to stay open in the case of a zip file.
            writer.flush();
        }
    }

    private static String generateHeader(Field[] fields) {
        return filterInstanceFields(fields).map(Field::getName)
                .map(GtfsScheduleWriter::camelToSnakeCase)
                .collect(Collectors.joining(DELIMITER));
    }

    private static <T> String generateDataLine(T item, Field[] fields) {
        return filterInstanceFields(fields).map(field -> {
            field.setAccessible(true);

            try {
                Object value = field.get(item);
                if (value instanceof LocalDate casted) {
                    return casted.format(DATE_FORMATTER);
                } else if (value instanceof Calendar.Type casted) {
                    return casted == Calendar.Type.AVAILABLE ? "1" : "0";
                } else if (value != null) {
                    return escapeCsv(value.toString());
                } else {
                    return NA_VALUE;
                }
            } catch (IllegalAccessException e) {
                log.error("Error accessing field value", e);
                return NA_VALUE;
            }

        }).collect(Collectors.joining(DELIMITER));
    }

    private static String escapeCsv(String value) {
        value = value.replace("\n", "").replace("\r", "");

        if (value.contains(DELIMITER) || value.contains("\"")) {
            // escape double quotes by doubling them
            value = value.replace("\"", "\"\"");

            // enclose the entire field in double quotes
            return "\"" + value + "\"";
        }

        return value;
    }

    private static Stream<Field> filterInstanceFields(Field[] fields) {
        return Arrays.stream(fields)
                .filter(field -> Modifier.isPrivate(field.getModifiers()) && !(Modifier.isStatic(
                        field.getModifiers()) && Modifier.isFinal(field.getModifiers())));
    }

    private static String camelToSnakeCase(String camelCase) {
        StringBuilder result = new StringBuilder();

        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    @Override
    public void save(GtfsSchedule result) throws IOException {
        Files.createDirectories(directory);

        if (zip) {
            Path zipFilePath = directory.resolve(GTFS_ZIP);
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                    Files.newOutputStream(zipFilePath, StandardOpenOption.CREATE))) {
                writeToZip(List.of(result.getFeedInfo()), zipOutputStream, GtfsFile.FEED_INFO);
                writeToZip(result.getAgencies(), zipOutputStream, GtfsFile.AGENCY);
                writeToZip(result.getStops(), zipOutputStream, GtfsFile.STOPS);
                writeToZip(result.getRoutes(), zipOutputStream, GtfsFile.ROUTES);
                writeToZip(result.getTrips(), zipOutputStream, GtfsFile.TRIPS);
                writeToZip(result.getStopTimes(), zipOutputStream, GtfsFile.STOP_TIMES);
                writeToZip(result.getCalendars(), zipOutputStream, GtfsFile.CALENDAR);
            }
        } else {
            writeToFile(List.of(result.getFeedInfo()), GtfsFile.FEED_INFO);
            writeToFile(result.getAgencies(), GtfsFile.AGENCY);
            writeToFile(result.getStops(), GtfsFile.STOPS);
            writeToFile(result.getRoutes(), GtfsFile.ROUTES);
            writeToFile(result.getTrips(), GtfsFile.TRIPS);
            writeToFile(result.getStopTimes(), GtfsFile.STOP_TIMES);
            writeToFile(result.getCalendars(), GtfsFile.CALENDAR);
        }
    }

    private <T> void writeToZip(List<T> list, ZipOutputStream zipOutputStream, GtfsFile gtfsFile) throws IOException {
        ZipEntry zipEntry = new ZipEntry(gtfsFile.fileName);
        zipOutputStream.putNextEntry(zipEntry);

        writeList(list, gtfsFile.clazz, zipOutputStream);

        zipOutputStream.closeEntry();
    }

    private <T> void writeToFile(List<T> list, GtfsFile gtfsFile) throws IOException {
        Path filePath = directory.resolve(gtfsFile.fileName);

        try (OutputStream writer = Files.newOutputStream(filePath, StandardOpenOption.CREATE)) {
            writeList(list, gtfsFile.clazz, writer);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    enum GtfsFile {
        AGENCY("agency.txt", Agency.class),
        STOPS("stops.txt", Stop.class),
        ROUTES("routes.txt", Route.class),
        TRIPS("trips.txt", Trip.class),
        STOP_TIMES("stop_times.txt", StopTime.class),
        CALENDAR("calendar.txt", Calendar.class),
        FEED_INFO("feed_info.txt", FeedInfo.class);

        private final String fileName;
        private final Class<?> clazz;
    }
}

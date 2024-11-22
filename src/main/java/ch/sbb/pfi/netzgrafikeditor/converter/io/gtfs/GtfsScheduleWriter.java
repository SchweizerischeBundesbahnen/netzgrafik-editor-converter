package ch.sbb.pfi.netzgrafikeditor.converter.io.gtfs;

import ch.sbb.pfi.netzgrafikeditor.converter.core.ConverterSink;
import ch.sbb.pfi.netzgrafikeditor.converter.gtfs.model.GtfsSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class GtfsScheduleWriter implements ConverterSink<GtfsSchedule> {

    private final Path directory;

    @Override
    public void save(GtfsSchedule result) throws IOException {
        Files.createDirectories(directory);

        writeListToFile(result.getRoutes(), "routes.txt");
        writeListToFile(result.getAgencies(), "agencies.txt");
        writeListToFile(result.getStops(), "stops.txt");
        writeListToFile(result.getTrips(), "trips.txt");
        writeListToFile(result.getStopTimes(), "stop_times.txt");
        writeListToFile(result.getCalendars(), "calendars.txt");
    }

    private <T> void writeListToFile(List<T> list, String fileName) throws IOException {
        if (list == null || list.isEmpty()) {
            return;
        }

        Path filePath = directory.resolve(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            Class<?> clazz = list.getFirst().getClass();
            Field[] fields = clazz.getDeclaredFields();

            // write header
            String header = generateHeader(fields);
            writer.write(header);
            writer.newLine();

            // write data
            for (T item : list) {
                String data = generateDataLine(item, fields);
                writer.write(data);
                writer.newLine();
            }
        }
    }

    private String generateHeader(Field[] fields) {
        return Arrays.stream(fields).map(Field::getName).map(this::camelToSnakeCase).collect(Collectors.joining(","));
    }

    private <T> String generateDataLine(T item, Field[] fields) {
        return Arrays.stream(fields).map(field -> {
            field.setAccessible(true);
            try {
                return field.get(item) != null ? field.get(item).toString() : "";
            } catch (IllegalAccessException e) {
                log.error("Error accessing field value", e);
                return "";
            }
        }).collect(Collectors.joining(","));
    }

    private String camelToSnakeCase(String camelCase) {
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
}

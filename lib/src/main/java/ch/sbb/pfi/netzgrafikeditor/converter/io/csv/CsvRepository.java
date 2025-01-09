package ch.sbb.pfi.netzgrafikeditor.converter.io.csv;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public abstract class CsvRepository<T> {

    protected final Map<String, T> entities = new HashMap<>();

    public CsvRepository(Path filePath, Function<CSVRecord, Entry<T>> entityMapper) throws IOException {
        log.info("Reading CSV file: {}", filePath);

        try (FileInputStream fileInputStream = new FileInputStream(
                filePath.toFile()); BOMInputStream bomInputStream = BOMInputStream.builder()
                .setInputStream(fileInputStream)
                .setByteOrderMarks(ByteOrderMark.UTF_8)
                .get(); InputStreamReader reader = new InputStreamReader(bomInputStream, StandardCharsets.UTF_8)) {

            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setIgnoreHeaderCase(true).setTrim(true).build();

            try (CSVParser csvParser = new CSVParser(reader, format)) {
                for (CSVRecord record : csvParser) {
                    Entry<T> entry = entityMapper.apply(record);
                    entities.put(entry.key, entry.value);
                }
            }
        }
    }

    protected T getEntity(String key) {
        T entity = entities.get(key);
        if (entity == null) {
            throw new IllegalArgumentException("No entry found in CSV for " + key);
        }

        return entity;
    }

    public record Entry<T>(String key, T value) {
    }
}

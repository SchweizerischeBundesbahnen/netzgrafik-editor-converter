package ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik;

import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class JsonDeserializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Deserializes JSON from a string into a NetworkGraphic object.
     *
     * @param jsonString the JSON string content
     * @return a NetworkGraphic object
     * @throws IOException if an error occurs during reading or parsing
     */
    public NetworkGraphic read(String jsonString) throws IOException {
        log.info("Reading netzgrafik from JSON string");

        // TODO: Validate input in deserializer? Remove spaces and dots?
        // network.getNodes().forEach(n -> n.set(n.getBetriebspunktName().replaceAll(" ", "_").replaceAll("\\.", "")));

        return objectMapper.readValue(jsonString, NetworkGraphic.class);
    }

    /**
     * Reads and deserializes a JSON file from a Path object using Files.readString.
     *
     * @param path the Path to the file
     * @return a NetworkGraphic object
     * @throws IOException if an error occurs during reading
     */
    public NetworkGraphic read(Path path) throws IOException {
        log.info("Reading netzgrafik from file: {}", path.toAbsolutePath());
        String fileContent = Files.readString(path, StandardCharsets.UTF_8);
        return read(fileContent);
    }

    /**
     * Reads and deserializes JSON from a URL.
     *
     * @param url the URL pointing to a JSON resource
     * @return a NetworkGraphic object
     * @throws IOException if an error occurs during reading
     */
    public NetworkGraphic read(URL url) throws IOException {
        log.info("Reading netzgrafik from URL: {}", url.toString());
        try (var inputStream = url.openStream()) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return read(content);
        }
    }
}

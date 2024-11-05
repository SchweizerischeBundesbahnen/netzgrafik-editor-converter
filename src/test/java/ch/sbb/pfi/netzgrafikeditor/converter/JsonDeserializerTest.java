package ch.sbb.pfi.netzgrafikeditor.converter;

import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class JsonDeserializerTest {

    private JsonDeserializer deserializer;


    @BeforeEach
    void setUp() {
        deserializer = new JsonDeserializer();
    }

    @Test
    void testRead_fromString() throws IOException {
        String jsonString = Files.readString(TestData.SIMPLE.getPath());

        NetworkGraphic networkGraphic = deserializer.read(jsonString);

        System.out.println(networkGraphic);
        assertNotNull(networkGraphic);
    }

    @Test
    void testRead_fromFile() throws IOException {
        Path filePath = TestData.SIMPLE.getPath();

        NetworkGraphic networkGraphic = deserializer.read(filePath);

        assertNotNull(networkGraphic);
    }

    @Test
    void testRead_fromURL() throws IOException {
        URL url = TestData.SIMPLE.getPath().toUri().toURL();

        NetworkGraphic networkGraphic = deserializer.read(url);

        assertNotNull(networkGraphic);
    }
}
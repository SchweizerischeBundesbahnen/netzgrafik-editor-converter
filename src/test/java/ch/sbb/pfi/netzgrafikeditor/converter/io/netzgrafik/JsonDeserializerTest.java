package ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik;

import ch.sbb.pfi.netzgrafikeditor.converter.TestCase;
import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

class JsonDeserializerTest {

    private JsonDeserializer deserializer;


    @BeforeEach
    void setUp() {
        deserializer = new JsonDeserializer();
    }

    @Test
    void testRead_fromString() throws IOException {
        String jsonString = Files.readString(TestCase.SIMPLE.getPath());

        NetworkGraphic networkGraphic = deserializer.read(jsonString);

        System.out.println(networkGraphic);
        Assertions.assertNotNull(networkGraphic);
    }

    @Test
    void testRead_fromFile() throws IOException {
        Path filePath = TestCase.SIMPLE.getPath();

        NetworkGraphic networkGraphic = deserializer.read(filePath);

        Assertions.assertNotNull(networkGraphic);
    }

    @Test
    void testRead_fromURL() throws IOException {
        URL url = TestCase.SIMPLE.getPath().toUri().toURL();

        NetworkGraphic networkGraphic = deserializer.read(url);

        Assertions.assertNotNull(networkGraphic);
    }
}
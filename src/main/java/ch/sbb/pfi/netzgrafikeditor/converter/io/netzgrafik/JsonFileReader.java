package ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik;

import ch.sbb.pfi.netzgrafikeditor.converter.NetworkGraphicSource;
import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;

@RequiredArgsConstructor
public class JsonFileReader implements NetworkGraphicSource {

    private final Path filePath;

    @Override
    public NetworkGraphic load() throws IOException {

        return new JsonDeserializer().read(filePath);

    }
}

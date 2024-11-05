package ch.sbb.pfi.netzgrafikeditor.converter.io.netzgrafik;

import ch.sbb.pfi.netzgrafikeditor.converter.NetworkGraphicSource;
import ch.sbb.pfi.netzgrafikeditor.converter.model.NetworkGraphic;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.file.Path;

@RequiredArgsConstructor
public class JsonFileReader implements NetworkGraphicSource {

    private final Path filePath;
    private volatile NetworkGraphic networkGraphic; // ensure visibility across threads

    @Override
    public NetworkGraphic load() throws IOException {

        // first check (without locking)
        if (networkGraphic == null) {
            synchronized (this) {
                // second check (with locking)
                if (networkGraphic == null) {
                    networkGraphic = new JsonDeserializer().read(filePath);
                }
            }
        }

        return networkGraphic;
    }
}

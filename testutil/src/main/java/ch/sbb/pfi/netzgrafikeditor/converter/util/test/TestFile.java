package ch.sbb.pfi.netzgrafikeditor.converter.util.test;

import lombok.RequiredArgsConstructor;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
public class TestFile {

    private final String folder;
    private final String fileName;

    /**
     * Returns the Path to the test file in the src/main/resources directory.
     *
     * @return Path to the test file
     */
    public Path getPath() {
        String resourcePath = String.format("%s/%s", folder, fileName);
        URL resourceUrl = getClass().getClassLoader().getResource(resourcePath);

        if (resourceUrl == null) {
            throw new IllegalStateException("Resource not found: " + resourcePath);
        }

        try {
            URI resourceUri = resourceUrl.toURI();
            return Paths.get(resourceUri);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Error converting URL to URI for resource: " + resourcePath, e);
        }
    }
}

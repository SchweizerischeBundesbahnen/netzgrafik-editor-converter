package ch.sbb.pfi.netzgrafikeditor.converter.test;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@RequiredArgsConstructor
public class TestFile {

    private final String folder;
    private final String fileName;

    /**
     * Returns the Path to a temporary copy of the test file in the src/main/resources directory.
     *
     * @return Path to the test file copy
     */
    public Path getPath() {
        String resourcePath = String.format("%s/%s", folder, fileName);

        try {
            // get resource from JAR
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IllegalStateException("Unable to open resource: " + resourcePath);
            }

            // copy to temporary file
            Path tempFile = Files.createTempFile(String.format("%s_", folder.replace("/", "_")),
                    String.format("_%s", fileName));
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            return tempFile;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

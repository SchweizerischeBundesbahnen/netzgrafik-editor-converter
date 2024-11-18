package ch.sbb.pfi.netzgrafikeditor.converter.core;

import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.nio.file.Paths;

@RequiredArgsConstructor
public class TestFile {

    private static final String RESOURCES_ROOT = "src/test/resources/";

    private final String folder;
    private final String fileName;

    /**
     * Returns the Path to the test file in the src/test/resources directory.
     *
     * @return Path to the test file
     */
    public Path getPath() {
        return Paths.get(RESOURCES_ROOT).resolve(folder).resolve(fileName);
    }
}

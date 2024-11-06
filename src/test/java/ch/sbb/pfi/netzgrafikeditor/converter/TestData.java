package ch.sbb.pfi.netzgrafikeditor.converter;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum TestData {

    SIMPLE("netzgrafik-simple.json"),
    CYCLE("netzgrafik-cycle.json"),
    CONFLICTING_TIMES("netzgrafik-conflicting-times.json");

    private final String fileName;

    TestData(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Returns the Path to the test file in the src/test/resources directory.
     *
     * @return Path to the test file
     */
    public Path getPath() {
        return Paths.get("src/test/resources/" + fileName);
    }
}

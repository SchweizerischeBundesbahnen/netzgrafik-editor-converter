package ch.sbb.pfi.netzgrafikeditor.converter;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum TestData {

    SIMPLE("ng-simple.json"),
    CYCLE("ng-cycle.json"),
    CONFLICTING_TIMES("ng-conflicting-times.json"),
    REALISTIC_SCENARIO("ng-realistic-scenario.json");

    private static final String RESOURCES_ROOT = "src/test/resources/";

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
        return Paths.get(RESOURCES_ROOT + fileName);
    }
}

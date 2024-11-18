package ch.sbb.pfi.netzgrafikeditor.converter.core;

import java.nio.file.Path;

public enum TestScenario {

    ALL_TEST_CASES("all-test-cases.json"),
    REALISTIC_SCENARIO("realistic.json");

    private static final String FOLDER = "ng/scenarios";

    private final TestFile testFile;

    TestScenario(String fileName) {
        this.testFile = new TestFile(FOLDER, fileName);
    }

    public Path getPath() {
        return testFile.getPath();
    }
}

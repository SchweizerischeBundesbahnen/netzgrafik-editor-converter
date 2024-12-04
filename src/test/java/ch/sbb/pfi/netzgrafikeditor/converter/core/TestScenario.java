package ch.sbb.pfi.netzgrafikeditor.converter.core;

import java.nio.file.Path;

public enum TestScenario {

    ALL_TEST_CASES("all-test-cases.json"),
    REALISTIC_SCENARIO("realistic.json");

    private static final String FOLDER = "ng/scenarios";

    public static final TestFile STOP_INFO_CSV = new TestFile(FOLDER, "stop_facility_info.csv");

    private final TestFile testFile;

    TestScenario(String fileName) {
        this.testFile = new TestFile(FOLDER, fileName);
    }

    public Path getPath() {
        return testFile.getPath();
    }
}

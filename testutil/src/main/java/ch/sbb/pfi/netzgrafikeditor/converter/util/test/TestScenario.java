package ch.sbb.pfi.netzgrafikeditor.converter.util.test;

import java.nio.file.Path;

public enum TestScenario {

    ALL_TEST_CASES("all-test-cases"),
    REALISTIC_SCENARIO("realistic");

    private static final String FOLDER = "ng/scenarios";

    private final TestFile networkGraphicFile;
    private final TestFile stopFacilityInfoCsvFile;
    private final TestFile rollingStockInfoCsvFile;

    TestScenario(String name) {
        this.networkGraphicFile = new TestFile(FOLDER, String.format("%s.json", name));
        this.stopFacilityInfoCsvFile = new TestFile(FOLDER, String.format("%s-stop-facility-info.csv", name));
        this.rollingStockInfoCsvFile = new TestFile(FOLDER, String.format("%s-rolling-stock-info.csv", name));
    }

    public Path getNetworkGraphicFilePath() {
        return networkGraphicFile.getPath();
    }

    public Path getStopFacilityInfoCsvFilePath() {
        return stopFacilityInfoCsvFile.getPath();
    }

    public Path getRollingStockInfoCsvFilePath() {
        return rollingStockInfoCsvFile.getPath();
    }
}

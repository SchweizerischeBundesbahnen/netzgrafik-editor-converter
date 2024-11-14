package ch.sbb.pfi.netzgrafikeditor.converter;

import java.nio.file.Path;
import java.nio.file.Paths;

public enum TestCase {

    SHORT("short.json"),
    SIMPLE("simple.json"),
    SIMPLE_PASS("simple-pass.json"),
    SIMPLE_DOUBLE_PASS("simple-double-pass.json"),
    SIMPLE_DWELL_TIME_CONFLICT("simple-dwell-time-conflict.json"),
    SIMPLE_RANDOM_ORDER("simple-random-order.json"),
    CYCLE_SMALL("cycle-small.json"),
    CYCLE("cycle.json"),
    CYCLE_RANDOM_ORDER("cycle-random-order.json"),
    SELF_INTERSECTION("self-intersection.json"),
    SELF_INTERSECTION_RANDOM_ORDER("self-intersection-random-order.json"),
    SELF_OVERLAY("self-overlay.json"),
    SELF_OVERLAY_RANDOM_ORDER("self-overlay-random-order.json"),
    REALISTIC_SCENARIO("realistic-scenario.json");

    private static final String RESOURCES_ROOT = "src/test/resources/ng/";

    private final String fileName;

    TestCase(String fileName) {
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

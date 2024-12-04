package ch.sbb.pfi.netzgrafikeditor.converter.util.test;

import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

@Getter
public enum TestCase {
    SHORT("short.json", StopSequence.SHORT),
    SIMPLE("simple.json", StopSequence.SIMPLE),
    SIMPLE_PASS("simple-pass.json", StopSequence.SIMPLE_PASS),
    SIMPLE_DOUBLE_PASS("simple-double-pass.json", StopSequence.SIMPLE_DOUBLE_PASS),
    SIMPLE_DWELL_TIME_CONFLICT("simple-dwell-time-conflict.json", StopSequence.SIMPLE),
    SIMPLE_RANDOM_ORDER("simple-random-order.json", StopSequence.SIMPLE.reversed()),
    CYCLE_SMALL("cycle-small.json", StopSequence.CYCLE_SMALL),
    CYCLE("cycle.json", StopSequence.CYCLE),
    CYCLE_RANDOM_ORDER("cycle-random-order.json", StopSequence.CYCLE.reversed()),
    SELF_INTERSECTION("self-intersection.json", StopSequence.SELF_INTERSECTION),
    SELF_INTERSECTION_RANDOM_ORDER("self-intersection-random-order.json", StopSequence.SELF_INTERSECTION),
    SELF_OVERLAY("self-overlay.json", StopSequence.SELF_OVERLAY.reversed()),
    SELF_OVERLAY_RANDOM_ORDER("self-overlay-random-order.json", StopSequence.SELF_OVERLAY);

    private static final String FOLDER = "ng/cases";

    public static final TestFile STOP_INFO_CSV = new TestFile(FOLDER, "stop_facility_info.csv");

    private final TestFile testFile;
    private final List<String> stopSequence;

    TestCase(String filename, List<String> stopSequence) {
        this.testFile = new TestFile(FOLDER, filename);
        this.stopSequence = stopSequence;
    }

    public Path getPath() {
        return testFile.getPath();
    }

    private static final class StopSequence {
        private static final List<String> SHORT = List.of("A", "B");
        private static final List<String> SIMPLE = List.of("A", "B", "C", "D");
        private static final List<String> SIMPLE_PASS = List.of("A", "C", "D");
        private static final List<String> SIMPLE_DOUBLE_PASS = List.of("A", "D");
        private static final List<String> CYCLE_SMALL = List.of("A", "B", "H", "A");
        private static final List<String> CYCLE = List.of("A", "B", "C", "D", "E", "F", "G", "H", "A");
        private static final List<String> SELF_INTERSECTION = List.of("A", "B", "G", "F", "C", "B", "H");
        private static final List<String> SELF_OVERLAY = List.of("A", "B", "G", "B", "C");
    }
}

package ch.sbb.pfi.netzgrafikeditor.converter.app;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = CommandLineConverter.class)
@ActiveProfiles("test")
class CommandLineConverterIT {

    private static final Path NETWORK_GRAPHIC_FILE = Path.of("src/test/resources/ng/scenarios/realistic.json");
    private static final String OUTPUT_ROOT = "integration-test/output/";
    private static final Path OUTPUT_PATH = Path.of(
            OUTPUT_ROOT + CommandLineConverterIT.class.getCanonicalName().replace(".", "/"));

    private final List<String> args = new ArrayList<>();

    private static void cleanup(Path path) throws IOException {
        if (Files.exists(path)) {
            try (Stream<Path> paths = Files.walk(path)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(file -> {
                    if (!file.delete()) {
                        throw new RuntimeException("Failed to delete " + file);
                    }
                });
            }
        }
    }

    @BeforeEach
    void setUp() {
        args.add(NETWORK_GRAPHIC_FILE.toString());
    }

    @ParameterizedTest
    @EnumSource(TestCase.class)
    void testConvertCommand_gtfs(TestCase testCase, CapturedOutput output) throws IOException {
        String format = "GTFS";
        runConverter(testCase, output, format);
    }

    @ParameterizedTest
    @EnumSource(TestCase.class)
    void testConvertCommand_matsim(TestCase testCase, CapturedOutput output) throws IOException {
        String format = "MATSIM";
        runConverter(testCase, output, format);
    }

    private void runConverter(TestCase testCase, CapturedOutput output, String format) throws IOException {
        // arrange
        Path outputPath = OUTPUT_PATH.resolve(format.toLowerCase()).resolve(testCase.name().toLowerCase());
        cleanup(outputPath);
        args.add(outputPath.toString());
        args.addAll(List.of("-f", format));
        args.addAll(Arrays.asList(testCase.args));

        // act
        int exitCode = SpringApplication.exit(
                SpringApplication.run(CommandLineConverter.class, args.toArray(new String[0])));

        // assert
        if (testCase.success) {
            assertThat(exitCode).isEqualTo(0);
            assertThat(Files.exists(outputPath)).isTrue();
            assertThat(Files.list(outputPath)).isNotEmpty();
        } else {
            assertThat(exitCode).isEqualTo(1);
            assertThat(Files.exists(outputPath)).isFalse();
            assertThat(output).contains(testCase.exception);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    private enum TestCase {
        VALIDATION_SKIP(new String[]{"-v", "SKIP_VALIDATION"}, true, ""),
        VALIDATION_WARN_ON_ISSUES(new String[]{"-v", "WARN_ON_ISSUES"}, true, ""),
        VALIDATION_FAIL_ON_ISSUES(new String[]{"-v", "FAIL_ON_ISSUES"}, false,
                "Found issues during network graphic validation and option fail on issue is set"),
        VALIDATION_FIX_ISSUES(new String[]{"-v", "FIX_ISSUES"}, true, ""),
        TRAIN_NAMES(new String[]{"-t"}, true, ""),
        SERVICE_DAY_START(new String[]{"-s", "03:30"}, true, ""),
        SERVICE_DAY_END(new String[]{"-s", "24:15"}, true, "");

        private final String[] args;
        private final boolean success;
        private final String exception;
    }
}

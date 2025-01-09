package ch.sbb.pfi.netzgrafikeditor.converter.app;

import ch.sbb.pfi.netzgrafikeditor.converter.test.OutputDir;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestDirectoryExtension;
import ch.sbb.pfi.netzgrafikeditor.converter.test.TestScenario;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(TestDirectoryExtension.class)
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest(classes = CommandLineConverter.class)
@ActiveProfiles("test")
class CommandLineConverterIT {

    private static final TestScenario TEST_SCENARIO = TestScenario.REALISTIC_SCENARIO;

    private final List<String> args = new ArrayList<>();
    private Path outputDir;

    @BeforeEach
    void setUp(@OutputDir Path outputDir) {
        this.outputDir = outputDir;
        args.add(TEST_SCENARIO.getNetworkGraphicFilePath().toString());
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
        args.add(outputDir.toString());
        args.addAll(List.of("-f", format));
        args.addAll(List.of("-i", TEST_SCENARIO.getStopFacilityInfoCsvFilePath().toString()));
        args.addAll(List.of("-r", TEST_SCENARIO.getRollingStockInfoCsvFilePath().toString()));
        args.addAll(Arrays.asList(testCase.args));

        // act
        int exitCode = SpringApplication.exit(
                SpringApplication.run(CommandLineConverter.class, args.toArray(new String[0])));

        // assert
        if (testCase.success) {
            assertThat(exitCode).isEqualTo(0);
            assertThat(Files.exists(outputDir)).isTrue();
            assertThat(Files.list(outputDir)).isNotEmpty();
        } else {
            assertThat(exitCode).isEqualTo(1);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(outputDir)) {
                assertThat(stream.iterator().hasNext()).isFalse();
            }
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
        VALIDATION_REPLACE_WHITESPACE(new String[]{"-v", "REPLACE_WHITESPACE"}, true, ""),
        VALIDATION_REMOVE_SPECIAL_CHARACTERS(new String[]{"-v", "REMOVE_SPECIAL_CHARACTERS"}, true, ""),
        TRAIN_NAMES(new String[]{"-t"}, true, ""),
        SERVICE_DAY_START(new String[]{"-s", "03:30"}, true, ""),
        SERVICE_DAY_END(new String[]{"-s", "24:15"}, true, "");

        private final String[] args;
        private final boolean success;
        private final String exception;
    }
}

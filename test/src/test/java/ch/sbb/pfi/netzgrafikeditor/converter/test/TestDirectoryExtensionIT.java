package ch.sbb.pfi.netzgrafikeditor.converter.test;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(TestDirectoryExtension.class)
class TestDirectoryExtensionIT {

    public static final String DUMMY_FILE = "dummy.txt";
    public static final String DUMMY_CONTENT = "Dummy content...";

    private static void runTestProcedure(Path inputRootDir, Path inputDir, Path outputDir) throws IOException {
        // input directories
        assertFalse(Files.isDirectory(inputRootDir));
        assertFalse(Files.isDirectory(inputDir));

        // ensure output directory has been created and is empty
        assertTrue(Files.isDirectory(outputDir));
        try (Stream<Path> files = Files.list(outputDir)) {
            assertTrue(files.toList().isEmpty());
        }

        // write dummy file
        writeDummyTxt(outputDir);
        assertTrue(Files.exists(outputDir.resolve(DUMMY_FILE)));
    }

    private static void writeDummyTxt(Path directory) throws IOException {
        Files.write(directory.resolve(DUMMY_FILE), DUMMY_CONTENT.getBytes(), StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Test
    public void testA(@InputRootDir Path inputRootDir, @InputDir Path inputDir, @OutputDir Path outputDir) throws IOException {
        runTestProcedure(inputRootDir, inputDir, outputDir);
    }

    @Test
    public void testB(@InputRootDir Path inputRootDir, @InputDir Path inputDir, @OutputDir Path outputDir) throws IOException {
        runTestProcedure(inputRootDir, inputDir, outputDir);
    }

    @Nested
    class NestedTest {

        @Test
        public void testC(@InputRootDir Path inputRootDir, @InputDir Path inputDir, @OutputDir Path outputDir) throws IOException {
            runTestProcedure(inputRootDir, inputDir, outputDir);
        }

        @Test
        public void testD(@InputRootDir Path inputRootDir, @InputDir Path inputDir, @OutputDir Path outputDir) throws IOException {
            runTestProcedure(inputRootDir, inputDir, outputDir);
        }

    }
}

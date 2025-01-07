package ch.sbb.pfi.netzgrafikeditor.converter.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Adds a test directory to the annotated test.
 * <p>
 * Each test case will have a directory structured by the fully specified class path and the method name. Nested tests
 * will create subdirectories. For example:
 * <pre>
 * integration-test/
 * ├── input/
 * │   └── com/example/SomeTest/
 * │       └── testMethod/
 * └── output/
 *     └── com/example/SomeTest/
 *         └── testMethod/
 * </pre>
 * <p>
 * Usage: Extend a test class with the extension and inject the input and output directories into the test methods. The
 * output directory will be created and emptied before each test execution.The input directory should be set up manually
 * and versioned (e.g., committed to a Git repository) to ensure consistency for each test case. The output directory is
 * managed by the test itself and should not be versioned (e.g. add to .gitignore).
 * <p>
 * Example:
 * <pre>
 * &#64ExtendWith(TestDirectoryExtension.class)
 * public class SomeTest {
 *
 *     &#64;Test
 *     void testMethod(&#64;InputRootDir Path inputRootDir, &#64;InputDir Path inputDir, &#64;OutputDir Path outputDir) {
 *
 *     }
 *
 * }
 * </pre>
 * <p>
 * Note: Use {@link BeforeEachCallback} instead of {@link BeforeTestExecutionCallback} to make the parameters available
 * in the {@link BeforeEach} annotated setup methods.
 * </p>
 */
public class TestDirectoryExtension implements BeforeEachCallback, ParameterResolver {

    private static final Path ROOT_PATH = Paths.get("../integration-test");
    private static final Path INPUT_BASE_PATH = ROOT_PATH.resolve("input");
    private static final Path OUTPUT_BASE_PATH = ROOT_PATH.resolve("output");

    private static final String OUTPUT_DIR = "outputDir";
    private static final String INPUT_DIR = "inputDir";

    private static String getClassPath(ExtensionContext context) {
        return context.getTestClass()
                .map(Class::getName)
                .orElseThrow(() -> new IllegalStateException("Test class not found"))
                .replace('.', '/')
                .replace("$", "/");
    }

    private static String getMethodName(ExtensionContext context) {
        return context.getTestMethod()
                .map(Method::getName)
                .orElseThrow(() -> new IllegalStateException("Test method not found"));
    }

    private static Path handleParameterizedTest(ExtensionContext context, Path outputDir) {
        if (context.getTestMethod()
                .map(method -> method.isAnnotationPresent(org.junit.jupiter.params.ParameterizedTest.class))
                .orElse(false)) {
            outputDir = outputDir.resolve(context.getDisplayName());
        }

        return outputDir;
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }

        Files.delete(path);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        String classPath = getClassPath(context);
        String methodName = getMethodName(context);

        Path inputDir = INPUT_BASE_PATH.resolve(classPath).resolve(methodName);
        Path outputDir = handleParameterizedTest(context, OUTPUT_BASE_PATH.resolve(classPath).resolve(methodName));

        context.getStore(ExtensionContext.Namespace.GLOBAL).put(INPUT_DIR, inputDir);
        context.getStore(ExtensionContext.Namespace.GLOBAL).put(OUTPUT_DIR, outputDir);

        if (Files.exists(outputDir)) {
            deleteDirectory(outputDir);
        }

        Files.createDirectories(outputDir);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return parameterContext.isAnnotated(InputRootDir.class) || parameterContext.isAnnotated(
                InputDir.class) || parameterContext.isAnnotated(OutputDir.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        ExtensionContext.Store store = extensionContext.getStore(ExtensionContext.Namespace.GLOBAL);

        if (parameterContext.isAnnotated(InputRootDir.class)) {
            return INPUT_BASE_PATH;
        } else if (parameterContext.isAnnotated(InputDir.class)) {
            return store.get(INPUT_DIR, Path.class);
        } else if (parameterContext.isAnnotated(OutputDir.class)) {
            return store.get(OUTPUT_DIR, Path.class);
        } else {
            throw new IllegalArgumentException("Unannotated parameter: " + parameterContext.getParameter().getName());
        }
    }
}

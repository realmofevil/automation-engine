package dev.realmofevil.automation.engine.reporting;

import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.config.OperatorConfig;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Stream;

public class AllureLifecycleManager {

    private static final Path RESULTS_DIR = Path.of(System.getProperty("allure.results.directory", "target/allure-results"));
    private static final Path HISTORY_DIR = Path.of("allure-history");

    public static void restoreHistory() {
        if (!Files.exists(HISTORY_DIR)) return;

        Path targetHistory = RESULTS_DIR.resolve("history");

        try {
            if (Files.exists(RESULTS_DIR) && !Files.isWritable(RESULTS_DIR)) {
                StepReporter.warn("Cannot restore history: Results directory is read-only.");
                return;
            }

            Files.createDirectories(targetHistory);

            try (Stream<Path> files = Files.walk(HISTORY_DIR)) {
                files.filter(Files::isRegularFile)
                     .forEach(src -> copySafely(src, targetHistory.resolve(HISTORY_DIR.relativize(src))));
            }
            StepReporter.info("Restored Allure history from " + HISTORY_DIR.toAbsolutePath());

        } catch (IOException e) {
            StepReporter.warn("Failed to restore Allure history: " + e.getMessage());
        }
    }

    private static void copySafely(Path src, Path dest) {
        try {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            StepReporter.warn("Failed to copy history file: " + src.getFileName());
        }
    }

    public static void writeEnvironment(EnvironmentConfig env, String suiteName) {
        Properties props = new Properties();
        props.setProperty("Environment", env.name());
        props.setProperty("Suite", suiteName == null ? "Unknown" : suiteName);
        props.setProperty("Java Version", System.getProperty("java.version"));
        props.setProperty("OS", System.getProperty("os.name"));

        StringBuilder ops = new StringBuilder();
        for (OperatorConfig op : env.operators()) {
            ops.append(op.id()).append(" ");
        }
        props.setProperty("Operators", ops.toString());

        try {
            Files.createDirectories(RESULTS_DIR);
            try (FileOutputStream fos = new FileOutputStream(RESULTS_DIR.resolve("environment.properties").toFile())) {
                props.store(fos, "Allure Environment Info");
            }
        } catch (IOException e) {
            StepReporter.warn("Failed to write Allure environment.properties: " + e.getMessage());
        }
    }

    /**
     * Clean results directory before run to avoid mixing history.
     */
    public static void cleanResults() {
        if (Files.exists(RESULTS_DIR)) {
            try (Stream<Path> walk = Files.walk(RESULTS_DIR)) {
                walk.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try { Files.delete(path); } catch (IOException ignored) {}
                    });
            } catch (IOException e) {
                StepReporter.warn("Could not clean old results: " + e.getMessage());
            }
        }
    }
}
package dev.realmofevil.automation.engine.reporting;

import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.config.OperatorConfig;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;
import java.util.stream.Stream;

public class AllureLifecycleManager {

    private static final Path RESULTS = Path.of("target/allure-results");
    private static final Path HISTORY = Path.of("allure-history");

    private static final Path[] REPORT_HISTORY_SOURCES = {
        Path.of("target/site/allure-maven-plugin/history"),
        Path.of("allure-report/history")
    };

    public static void restoreHistory() {
        if (!Files.exists(HISTORY)) return;

        try {
            Path resultsHistory = RESULTS.resolve("history");
            Files.createDirectories(resultsHistory);

            try (Stream<Path> files = Files.walk(HISTORY)) {
                files.filter(Files::isRegularFile)
                     .forEach(src -> {
                         try {
                             Files.copy(src, resultsHistory.resolve(HISTORY.relativize(src)), StandardCopyOption.REPLACE_EXISTING);
                         } catch (IOException e) {
                             StepReporter.warn("Failed to copy history file: " + src);
                         }
                     });
            }
            StepReporter.info("Restored Allure history from " + HISTORY.toAbsolutePath());
        } catch (IOException e) {
            StepReporter.warn("Could not restore Allure history: " + e.getMessage());
        }
    }

    /**
     * Persists history from the generated report back to the project root.
     * Useful for local runs to maintain trend data across 'mvn clean' executions.
     */
    public static void saveHistory() {
        for (Path source : REPORT_HISTORY_SOURCES) {
            if (Files.exists(source) && Files.isDirectory(source)) {
                try {
                    Files.createDirectories(HISTORY);
                    
                    try (Stream<Path> files = Files.walk(source)) {
                        files.filter(Files::isRegularFile)
                             .forEach(src -> {
                                 try {
                                     Files.copy(src, HISTORY.resolve(source.relativize(src)), StandardCopyOption.REPLACE_EXISTING);
                                 } catch (IOException e) {
                                     StepReporter.warn("Failed to save history file: " + src);
                                 }
                             });
                    }
                    StepReporter.info("Saved Allure history from " + source + " to " + HISTORY);
                    return;
                } catch (IOException e) {
                    StepReporter.warn("Failed to save Allure history: " + e.getMessage());
                }
            }
        }
        StepReporter.info("No generated report history found to save. (Run 'mvn allure:report' first)");
    }

    public static void writeEnvironment(EnvironmentConfig env, String suiteName) {
        Properties props = new Properties();
        props.setProperty("Environment", env.name());
        props.setProperty("Suite", suiteName == null ? "Unknown" : suiteName);
        props.setProperty("Java Version", System.getProperty("java.version"));

        // List Operators
        StringBuilder ops = new StringBuilder();
        for (OperatorConfig op : env.operators()) {
            ops.append(op.id()).append(", ");
        }
        props.setProperty("Operators", ops.toString());

        try {
            Files.createDirectories(RESULTS);
            try (FileOutputStream fos = new FileOutputStream(RESULTS.resolve("environment.properties").toFile())) {
                props.store(fos, "Allure Environment");
            }
        } catch (IOException e) {
            StepReporter.warn("Failed to write Allure environment properties.");
        }
    }
}
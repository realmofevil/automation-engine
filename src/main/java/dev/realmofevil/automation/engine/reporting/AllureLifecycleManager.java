package dev.realmofevil.automation.engine.reporting;

import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.config.OperatorConfig;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Properties;

public class AllureLifecycleManager {

    private static final Path RESULTS = Path.of("target/allure-results");
    private static final Path HISTORY = Path.of("allure-history"); // Persisted folder

    public static void restoreHistory() {
        if (Files.exists(HISTORY)) {
            try {
                if (!Files.exists(RESULTS.resolve("history"))) {
                    Files.createDirectories(RESULTS.resolve("history"));
                }
                Files.walk(HISTORY).filter(Files::isRegularFile).forEach(src -> {
                    try {
                        Files.copy(src, RESULTS.resolve("history").resolve(HISTORY.relativize(src)), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        StepReporter.warn("Failed to restore history file: " + src);
                    }
                });
            } catch (IOException e) {
                StepReporter.warn("Could not restore Allure history.");
            }
        }
    }

    public static void saveHistory() {
        // usually handled by the CI pipeline (mvn allure:report), 
        // but if running locally, we can copy target/site/allure-maven-plugin/history back to allure-history
        // and best left to the CI artifacts.
    }

    public static void writeEnvironment(EnvironmentConfig env, String suiteName) {
        Properties props = new Properties();
        props.setProperty("Environment", env.name());
        props.setProperty("Suite", suiteName);
        props.setProperty("Java Version", System.getProperty("java.version"));

        StringBuilder ops = new StringBuilder();
        for(OperatorConfig op : env.operators()) {
            ops.append(op.id()).append(", ");
        }
        props.setProperty("Operators", ops.toString());

        try {
            Files.createDirectories(RESULTS);
            try (FileOutputStream fos = new FileOutputStream(RESULTS.resolve("environment.properties").toFile())) {
                props.store(fos, "Allure Environment Variables");
            }
        } catch (IOException e) {
            StepReporter.warn("Failed to write Allure environment properties.");
        }
    }
}
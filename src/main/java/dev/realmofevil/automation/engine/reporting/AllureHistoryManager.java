package dev.realmofevil.automation.engine.reporting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AllureHistoryManager {

    /**
    private static final Path RESULTS = Path.of("target/allure-results");
    private static final Path REPORT = Path.of("target/allure-report");

    private AllureHistoryManager() {}

    public static void restoreHistory() {
        Path history = REPORT.resolve("history");
        if (Files.exists(history)) {
            try {
                Files.createDirectories(RESULTS);
                Files.walk(history).forEach(src -> {
                    try {
                        Path dest = RESULTS.resolve("history")
                                .resolve(history.relativize(src));
                        Files.copy(src, dest);
                    } catch (Exception ignored) {
                    }
                });
            } catch (Exception ignored) {
            }
        }
    }
    **/
    
    private static final Path PERSISTED_HISTORY = Path.of("allure-history");

    private static final Path RESULTS_HISTORY = Path.of("allure-results", "history");

    private AllureHistoryManager() {
    }

    /**
     * Restores history BEFORE tests start
     */
    public static void restoreHistory() {
        if (!Files.exists(PERSISTED_HISTORY)) {
            return;
        }

        try {
            Files.createDirectories(RESULTS_HISTORY);

            Files.walk(PERSISTED_HISTORY)
                    .filter(Files::isRegularFile)
                    .forEach(source -> copyToResults(source));

        } catch (Exception e) {
            throw new RuntimeException("Failed to restore Allure history", e);
        }
    }

    /**
     * Persists history AFTER tests finish
     */
    public static void persistHistory() {
        if (!Files.exists(RESULTS_HISTORY)) {
            return;
        }

        try {
            Files.createDirectories(PERSISTED_HISTORY);

            Files.walk(RESULTS_HISTORY)
                    .filter(Files::isRegularFile)
                    .forEach(source -> copyToPersisted(source));

        } catch (Exception e) {
            throw new RuntimeException("Failed to persist Allure history", e);
        }
    }

    private static void copyToResults(Path source) {
        try {
            Path target = RESULTS_HISTORY.resolve(source.getFileName());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void copyToPersisted(Path source) {
        try {
            Path target = PERSISTED_HISTORY.resolve(source.getFileName());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

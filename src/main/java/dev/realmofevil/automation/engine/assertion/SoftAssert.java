package dev.realmofevil.automation.engine.assertion;

import dev.realmofevil.automation.engine.reporting.StepReporter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SoftAssert {
    private final List<Throwable> errors = new ArrayList<>();

    /**
     * Executes a check. If it fails, logs the error but continues execution.
     */
    public void check(String description, Runnable assertion) {
        try {
            assertion.run();
        } catch (Throwable e) {
            String msg = "Soft Assert Failed: " + description;
            StepReporter.warn(msg + " -> " + e.getMessage());
            errors.add(new AssertionError(msg, e));
        }
    }

    /**
     * Call this at the end of the test. If any checks failed, it throws an exception.
     */
    public void assertAll() {
        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Multiple failures in test:\n");
            for (Throwable t : errors) {
                sb.append(" - ").append(t.getMessage()).append("\n");
            }
            throw new AssertionError(sb.toString());
        }
    }
}
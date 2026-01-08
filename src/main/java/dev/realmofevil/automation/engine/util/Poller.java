package dev.realmofevil.automation.engine.util;

import dev.realmofevil.automation.engine.reporting.StepReporter;
import io.qameta.allure.Allure;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class Poller {

    public static <T> T await(String description, Callable<T> supplier, Predicate<T> condition) {
        return await(description, supplier, condition, Duration.ofSeconds(10), Duration.ofMillis(500));
    }

    public static <T> T await(String description, Callable<T> supplier, Predicate<T> condition, Duration timeout,
            Duration pollInterval) {
        long end = System.currentTimeMillis() + timeout.toMillis();

        return Allure.step("Polling: " + description + " (" + timeout.toSeconds() + "s)", () -> {
            Throwable lastError = null;

            while (System.currentTimeMillis() < end) {
                try {
                    T result = supplier.call();
                    if (condition.test(result)) {
                        StepReporter.info("Polling successful: " + description);
                        return result;
                    }
                } catch (Exception e) {
                    lastError = e;
                }

                try {
                    Thread.sleep(pollInterval.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Polling interrupted");
                }
            }

            String msg = "Timeout waiting for: " + description;
            StepReporter.error(msg, lastError);
            throw new AssertionError(msg, lastError);
        });
    }
}
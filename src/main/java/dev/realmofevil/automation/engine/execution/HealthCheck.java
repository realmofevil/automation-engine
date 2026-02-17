package dev.realmofevil.automation.engine.execution;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.reporting.StepReporter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HealthCheck {

    private static final Map<String, Status> CACHE = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(1);

    public static void verify(ExecutionContext ctx) {
        String operatorId = ctx.config().id();
        Status lastStatus = CACHE.get(operatorId);

        if (lastStatus != null &&
                Duration.between(lastStatus.timestamp, Instant.now()).compareTo(CACHE_TTL) < 0) {
            return;
        }

        synchronized (operatorId.intern()) {
            lastStatus = CACHE.get(operatorId);
            if (lastStatus != null && Duration.between(lastStatus.timestamp, Instant.now()).compareTo(CACHE_TTL) < 0) {
                return;
            }

            try {
                int status = ctx.api().url(ctx.config().domains().desktopUri().toString())
                        .get().extract().status();

                if (status >= 500) {
                    throw new RuntimeException("Environment Health Check Failed: HTTP " + status);
                }

                StepReporter.info("Health Check Passed: " + operatorId + " is reachable with status code " + status);
                CACHE.put(operatorId, new Status(true, Instant.now()));

            } catch (Exception e) {
                StepReporter.warn("Health Check Failed: " + e.getMessage() + ". Proceeding with caution.");
            }
        }
    }

    private record Status(boolean up, Instant timestamp) {
    }
}
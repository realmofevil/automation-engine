package dev.realmofevil.automation.engine.suite.model;

public record FlakinessPolicy(
        int retries,
        boolean failOnFlaky,
        Quarantine quarantine
) {
    public record Quarantine(
            boolean enabled,
            int maxFlakyRatePercent
    ) {}
}
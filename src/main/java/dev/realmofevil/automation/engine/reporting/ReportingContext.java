package dev.realmofevil.automation.engine.reporting;

import java.util.UUID;

public record ReportingContext(
        String environment,
        String operator,
        String suite,
        UUID executionId
) {}

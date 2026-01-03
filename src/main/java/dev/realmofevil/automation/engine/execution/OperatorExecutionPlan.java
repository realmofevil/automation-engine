package dev.realmofevil.automation.engine.execution;

import dev.realmofevil.automation.engine.config.OperatorEndpoint;

public record OperatorExecutionPlan(
        String operator,
        OperatorEndpoint endpoint
) {}

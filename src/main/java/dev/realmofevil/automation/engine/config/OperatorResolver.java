package dev.realmofevil.automation.engine.config;

import dev.realmofevil.automation.engine.context.ExecutionContext;

import java.util.List;
import java.util.Map;

public final class OperatorResolver {

    private OperatorResolver() {
    }

    public static List<String> resolveOperators() {
        ExecutionConfig config = ExecutionContext.get();
        String operator = config.suite().operator();

        Map<String, OperatorEndpoint> available = config.environment().operators();

        if (operator == null || operator.equalsIgnoreCase("generic")) {
            return List.of();
        }

        if (operator.equalsIgnoreCase("all")) {
            return available.keySet().stream().toList();
        }

        if (!available.containsKey(operator)) {
            throw new IllegalArgumentException(
                    "Operator not defined for environment: " + operator);
        }

        return List.of(operator);
    }
}

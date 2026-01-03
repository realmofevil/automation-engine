package dev.realmofevil.automation.engine.config;

import java.util.Map;

public record EnvironmentConfig(
        String name,
        Map<String, OperatorEndpoint> operators, // String baseDomain,
        int timeoutMs
) {}

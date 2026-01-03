package dev.realmofevil.automation.engine.config;

import java.util.List;

public record EnvironmentConfig(
    String name,
    OperatorConfig defaults,
    List<OperatorConfig> operators
) {}
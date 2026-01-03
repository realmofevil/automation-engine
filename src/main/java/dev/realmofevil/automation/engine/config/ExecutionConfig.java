package dev.realmofevil.automation.engine.config;

public record ExecutionConfig(
        EnvironmentConfig environment,
        OperatorConfig operator,
        RoutesConfig routes,
        SuiteConfig suite
) {}

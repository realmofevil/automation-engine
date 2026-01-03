package dev.realmofevil.automation.engine.config;

import java.util.Map;

public record RoutesConfig(
        String basePath,
        Map<String, String> endpoints
) {}

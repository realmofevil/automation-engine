package dev.realmofevil.automation.engine.config;

import java.util.Map;

public final class ConfigValidator {

    private ConfigValidator() {}

    public static void validate(EnvironmentConfig env) {
        require(env.name(), "environment.name");
        require(env.operators(), "environment.operators");

        if (env.operators().isEmpty()) {
            throw new ConfigurationException("No operators configured");
        }

        for (Map.Entry<String, OperatorEndpoint> entry : env.operators().entrySet()) {
            String operator = entry.getKey();
            OperatorEndpoint endpoint = entry.getValue();

            require(endpoint.scheme(), operator + ".scheme");
            require(endpoint.domain(), operator + ".domain");

            if (endpoint.basePath() == null) {
                throw new ConfigurationException(
                        "Missing basePath for operator: " + operator
                );
            }

            if (endpoint.parallelism() < 1) {
                throw new ConfigurationException(
                        "Invalid parallelism for operator: " + operator
                );
            }
        }
    }

    private static void require(Object value, String name) {
        if (value == null) {
            throw new ConfigurationException("Missing required config: " + name);
        }
        if (value instanceof String s && s.isBlank()) {
            throw new ConfigurationException("Blank config value: " + name);
        }
    }
}

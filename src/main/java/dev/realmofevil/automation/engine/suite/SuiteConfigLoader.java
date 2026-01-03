package dev.realmofevil.automation.engine.suite;

import dev.realmofevil.automation.engine.config.*;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public final class SuiteConfigLoader {

    private SuiteConfigLoader() {}

    @SuppressWarnings("unchecked")
    public static ExecutionConfig load(Map<String, String> overrides) {

        Yaml yaml = new Yaml();
        InputStream in =
                SuiteConfigLoader.class
                        .getClassLoader()
                        .getResourceAsStream("suite.yml");

        Map<String, Object> root = yaml.load(in);
        Map<String, Object> s = (Map<String, Object>) root.get("suite");

        String envName = ConfigMerger.override(
                "env",
                (String) s.get("environment"),
                overrides
        );

        String operatorName = ConfigMerger.override(
                "operator",
                (String) s.get("operator"),
                overrides
        );

        SuiteConfig suite = new SuiteConfig(
                (String) s.get("name"),
                (java.util.List<String>) s.get("includeTags"),
                (java.util.List<String>) s.get("excludeTags"),
                ConfigMerger.overrideInt(
                        "parallelism",
                        (Integer) s.get("parallelism"),
                        overrides
                ),
                envName,
                operatorName
        );

        EnvironmentConfig env =
                ConfigLoader.load("env/" + envName + ".yml",
                        EnvironmentWrapper.class).environment;

        OperatorConfig operator =
                operatorName == null
                        ? null
                        : ConfigLoader.load(
                                "operators/" + operatorName + ".yml",
                                OperatorWrapper.class).operator;

        RoutesConfig routes =
                ConfigLoader.load("routes/services.yml",
                        RoutesWrapper.class).routes;

        ExecutionConfig config =
                new ExecutionConfig(env, operator, routes, suite);

        ExecutionContext.set(config);
        return config;
    }

    private static class EnvironmentWrapper {
        public EnvironmentConfig environment;
    }

    private static class OperatorWrapper {
        public OperatorConfig operator;
    }

    private static class RoutesWrapper {
        public RoutesConfig routes;
    }
}

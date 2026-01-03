package dev.realmofevil.automation.engine.config;

import java.util.List;

public record SuiteConfig(
        String name,
        List<String> includeTags,
        List<String> excludeTags,
        int parallelism,
        String environment,
        String operator
) {}

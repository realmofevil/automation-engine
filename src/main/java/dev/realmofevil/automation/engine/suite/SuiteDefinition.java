package dev.realmofevil.automation.engine.suite;

import java.util.List;

public record SuiteDefinition(
        String name,
        List<String> operators,
        List<TestDefinition> tests
) {}
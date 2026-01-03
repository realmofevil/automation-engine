package dev.realmofevil.automation.engine.suite;

import java.util.List;

public record TestDefinition(
        String className,
        List<String> tags
) {}
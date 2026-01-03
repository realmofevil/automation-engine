package dev.realmofevil.automation.engine.config;

import java.util.List;

public record SuiteDefinition(
        String name,
        List<String> targetOperators, // or "ALL"
        List<TestEntry> tests) {
    public record TestEntry(String className, List<String> tags) {
    }
}
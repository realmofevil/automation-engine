package dev.realmofevil.automation.engine.bootstrap;

import java.util.Map;

public final class CliOverrides {

    private final Map<String, String> values;

    public CliOverrides(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    public String get(String key) {
        return values.get(key);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }
}
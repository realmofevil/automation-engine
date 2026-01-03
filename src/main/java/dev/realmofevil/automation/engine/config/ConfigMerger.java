package dev.realmofevil.automation.engine.config;

import java.util.Map;

public final class ConfigMerger {

    private ConfigMerger() {
    }

    public static String override(
            String key,
            String current,
            Map<String, String> overrides) {
        return overrides.getOrDefault(key, current);
    }

    public static int overrideInt(
            String key,
            int current,
            Map<String, String> overrides) {
        return overrides.containsKey(key)
                ? Integer.parseInt(overrides.get(key))
                : current;
    }
}

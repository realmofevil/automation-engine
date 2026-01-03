package dev.realmofevil.automation.engine.bootstrap;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class CliArguments {

    private final Map<String, String> values;

    private CliArguments(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    public static CliArguments parse(String[] args) {
        Objects.requireNonNull(args, "args");

        Map<String, String> parsed = new HashMap<>();

        for (String arg : args) {
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException(
                        "Invalid CLI argument (expected --key=value): " + arg
                );
            }

            String[] parts = arg.substring(2).split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid CLI argument format: " + arg
                );
            }

            parsed.put(parts[0], parts[1]);
        }

        return new CliArguments(parsed);
    }

    public String get(String key) {
        return values.get(key);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(values);
    }
}
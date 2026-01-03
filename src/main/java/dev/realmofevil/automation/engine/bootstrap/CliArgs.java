package dev.realmofevil.automation.engine.bootstrap;

import java.util.HashMap;
import java.util.Map;

public final class CliArgs {
    public static Map<String, String> parse(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] p = arg.substring(2).split("=", 2);
                map.put(p[0], p[1]);
            }
        }
        return map;
    }
}
package dev.realmofevil.automation.engine.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionState {

    private final Map<String, String> values = new ConcurrentHashMap<>();

    public void put(String key, String value) {
        values.put(key, value);
    }

    public String get(String key) {
        return values.get(key);
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }
}
package dev.realmofevil.automation.engine.routing;

import java.util.HashMap;
import java.util.Map;

public final class RouteCatalog {

    private final Map<String, RouteDefinition> routes;

    public RouteCatalog(Map<String, String> rawRoutes) {
        this.routes = new HashMap<>();
        rawRoutes.forEach((key, value) -> {
            String method = "ANY";
            String path = value;

            String[] parts = value.trim().split("\\s+", 2);
            if (parts.length == 2) {
                String potentialMethod = parts[0].toUpperCase();
                if (isHttpVerb(potentialMethod)) {
                    method = potentialMethod;
                    path = parts[1];
                }
            }
            // Logic: Value is just the path (e.g. "/api/login")
            // The Method is determined by the Test at runtime (.post(), .get())
            // We store "ANY" or null as the method in the definition.
            this.routes.put(key, new RouteDefinition(method, path));
        });
    }

    private boolean isHttpVerb(String s) {
        return s.equals("GET") || s.equals("POST") || s.equals("PUT") || s.equals("DELETE") || s.equals("PATCH");
    }

    public RouteDefinition get(String key) {
        RouteDefinition def = routes.get(key);
        if (def == null) {
            throw new IllegalArgumentException("Unknown route key: " + key);
        }
        return def;
    }
}
package dev.realmofevil.automation.engine.routing;

import java.util.HashMap;
import java.util.Map;

public final class RouteCatalog {

    private final Map<String, RouteDefinition> routes;

    public RouteCatalog(Map<String, String> rawRoutes) {
        this.routes = new HashMap<>();
        rawRoutes.forEach((key, value) -> {
            // Logic: Value is just the path (e.g. "/api/login")
            // The Method is determined by the Test at runtime (.post(), .get())
            // We store "ANY" or null as the method in the definition.
            this.routes.put(key, new RouteDefinition("ANY", value));
        });
    }

    public RouteDefinition get(String key) {
        RouteDefinition def = routes.get(key);
        if (def == null) {
            throw new IllegalArgumentException("Unknown route key: " + key);
        }
        return def;
    }
}
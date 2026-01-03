package dev.realmofevil.automation.engine.routing;

import java.util.Map;

public final class RouteCatalog {

    private final Map<String, RouteDefinition> routes;

    public RouteCatalog(Map<String, RouteDefinition> routes) {
        this.routes = Map.copyOf(routes);
    }

    public RouteDefinition get(String key) {
        RouteDefinition def = routes.get(key);
        if (def == null) {
            throw new IllegalArgumentException("Unknown route: " + key);
        }
        return def;
    }
}
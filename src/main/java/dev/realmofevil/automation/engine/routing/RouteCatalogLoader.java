package dev.realmofevil.automation.engine.routing;

import dev.realmofevil.automation.engine.bootstrap.YamlLoader;

import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.Map;

public final class RouteCatalogLoader {

    private RouteCatalogLoader() {}

    @SuppressWarnings("unchecked")
    public static RouteCatalog load(String resourcePath) {
        Map<String, Object> root = YamlLoader.load("routes/" + resourcePath);
        Map<String, Object> routes =
                (Map<String, Object>) root.get("routes");

        Map<String, RouteDefinition> resolved = new HashMap<>();

        routes.forEach((key, value) -> {
            Map<String, String> def = (Map<String, String>) value;
            resolved.put(key,
                    new RouteDefinition(
                            HttpRequest.Method.valueOf(def.get("method")),
                            def.get("path")
                    ));
        });

        return new RouteCatalog(resolved);
    }
}
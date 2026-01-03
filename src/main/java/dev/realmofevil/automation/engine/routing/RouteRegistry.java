package dev.realmofevil.automation.engine.routing;

public final class RouteRegistry {

    private final String basePath;

    public RouteRegistry(String basePath) {
        this.basePath = basePath;
    }

    public String basePath() {
        return basePath;
    }

    public String resolve(String path) {
        return basePath + path;
    }
}
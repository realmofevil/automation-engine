package dev.realmofevil.automation.engine.config;

public record OperatorEndpoint(
        String scheme,
        String domain,
        String basePath,
        int parallelism,
        DatabaseConfig database,
        AuthConfig auth
) {
    public String baseUrl() {
        return scheme + "://" + domain + basePath;
    }
}

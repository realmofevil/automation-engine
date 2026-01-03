package core.config;

import core.environment.Environment;
import core.environment.Platform;

import java.net.URI;

public final class ConfigLoader {

    private ConfigLoader() {}

    public static FrameworkConfig load() {
        String env = System.getProperty("environment", "STAGING");
        String platform = System.getProperty("platform", "DESKTOP");
        String baseUrl = System.getProperty("baseUrl", "https://api.example.com");

        return new FrameworkConfig(
                Environment.valueOf(env.toUpperCase()),
                Platform.valueOf(platform.toUpperCase()),
                URI.create(baseUrl),
                30
        );
    }
}
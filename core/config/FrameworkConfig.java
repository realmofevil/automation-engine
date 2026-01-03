package core.config;

import core.environment.Environment;
import core.environment.Platform;
import java.net.URI;

public record FrameworkConfig(
        Environment environment,
        Platform platform,
        URI baseUrl,
        int timeoutSeconds
) {}
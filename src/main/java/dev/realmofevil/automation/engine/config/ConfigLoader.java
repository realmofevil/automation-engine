package dev.realmofevil.automation.engine.config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;

/**
 * Loads environment configuration from YAML files.
 *
 * Expected location:
 *   src/main/resources/env/{environment}.yml
 *
 * Example:
 *   -Dserver=qa  -> env/qa.yml
 */
public final class ConfigLoader {

    private ConfigLoader() {}

    public static EnvironmentConfig load(String environment) {
        String path = "env/" + environment + ".yml";

        InputStream inputStream =
                ConfigLoader.class
                        .getClassLoader()
                        .getResourceAsStream(path);

        if (inputStream == null) {
            throw new ConfigurationException(
                    "Environment config not found: " + path
            );
        }

        Yaml yaml = new Yaml(new Constructor(EnvironmentConfig.class));
        EnvironmentConfig config = yaml.load(inputStream);

        if (config == null) {
            throw new ConfigurationException(
                    "Failed to load environment config: " + path
            );
        }

        return config;
    }
}

/**
// Alternative implementation using a reusable YAML instance
package dev.realmofevil.automation.engine.config;

import dev.realmofevil.automation.engine.util.ResourceLoader;
import org.yaml.snakeyaml.Yaml;

public final class ConfigLoader {

    private static final Yaml YAML = new Yaml();

    private ConfigLoader() {}

    public static <T> T load(String path, Class<T> type) {
        return YAML.loadAs(
                ResourceLoader.load(path),
                type);
    }
}
**/
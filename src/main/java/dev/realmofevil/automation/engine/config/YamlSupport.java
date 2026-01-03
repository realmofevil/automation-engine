package dev.realmofevil.automation.engine.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

/**
 * Factory for creating configured SnakeYAML instances.
 */
public final class YamlSupport {

    private YamlSupport() {}

    public static <T> Yaml create(Class<T> type) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        // Essential for parsing Java Records
        return new Yaml(new Constructor(type, options));
    }
}
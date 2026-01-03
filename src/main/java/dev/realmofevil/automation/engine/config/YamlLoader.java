package dev.realmofevil.automation.engine.config;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;

public final class YamlLoader {
    private static final Yaml YAML = new Yaml();
    public static <T> T load(InputStream is, Class<T> type) {
        return YAML.loadAs(is, type);
    }
}
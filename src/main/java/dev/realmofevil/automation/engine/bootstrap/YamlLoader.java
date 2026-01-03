package dev.realmofevil.automation.engine.bootstrap;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.InputStream;
import java.util.Map;

public final class YamlLoader {

    private static final Load LOAD =
            new Load(LoadSettings.builder().build());

    private YamlLoader() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> load(String resourcePath) {
        InputStream is = YamlLoader.class
                .getClassLoader()
                .getResourceAsStream(resourcePath);

        if (is == null) {
            throw new IllegalArgumentException(
                    "YAML resource not found: " + resourcePath
            );
        }

        return (Map<String, Object>) LOAD.loadFromInputStream(is);
    }
}
package dev.realmofevil.automation.engine.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Factory for creating configured Jackson YAML Mappers.
 * Replaces SnakeYAML for better Java Record support.
 */
public final class YamlSupport {
    private static final ObjectMapper MAPPER;

    static {
        MAPPER = new ObjectMapper(new YAMLFactory());
        MAPPER.findAndRegisterModules();
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private YamlSupport() {}

    public static <T> T load(java.io.InputStream is, Class<T> type) {
        try {
            return MAPPER.readValue(is, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse YAML into " + type.getSimpleName(), e);
        }
    }
    
    public static <T> T load(java.util.Map<String, Object> map, Class<T> type) {
        return MAPPER.convertValue(map, type);
    }
}
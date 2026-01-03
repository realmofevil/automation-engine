package dev.realmofevil.automation.engine.bootstrap;

import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.config.SuiteDefinition;
import dev.realmofevil.automation.engine.config.YamlSupport;
import dev.realmofevil.automation.engine.routing.RouteCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Central loading point for all YAML configurations.
 * Handles classpath resource resolution.
 */
public final class ConfigLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);

    private ConfigLoader() {}

    /**
     * Loads environment config (e.g., env/dev.yaml)
     */
    public static EnvironmentConfig loadEnv(String envName) {
        String path = "env/" + envName + ".yaml";
        EnvironmentConfig raw = loadInternal(path, EnvironmentConfig.class);
        List<OperatorConfig> mergedOperators = raw.operators().stream()
            .map(op -> merge(op, raw.defaults()))
            .toList();
        LOG.info("Loading environment configuration from: {}", path);
        return new EnvironmentConfig(raw.name(), raw.defaults(), mergedOperators);
    }

    private static OperatorConfig merge(OperatorConfig specific, OperatorConfig defaults) {
        if (defaults == null) return specific;

        return new OperatorConfig(
            specific.id(),
            specific.environment(),
            specific.domains(),
            specific.accounts(),
            specific.db() != null ? specific.db() : defaults.db(),
            specific.routeCatalog() != null ? specific.routeCatalog() : defaults.routeCatalog(),
            specific.parallelism() > 0 ? specific.parallelism() : defaults.parallelism(),
            (specific.auth() != null && !specific.auth().isEmpty()) ? specific.auth() : defaults.auth()
        );
    }

    /**
     * Loads suite definition (e.g., suites/smoke.yaml)
     */
    public static SuiteDefinition loadSuite(String suiteName) {
        String path = "suites/" + suiteName + ".yaml";
        LOG.info("Loading suite definition from: {}", path);
        return loadInternal(path, SuiteDefinition.class);
    }

    /**
     * Loads route catalog (e.g., routes/payment.yaml)
     * Maps raw YAML map to the RouteCatalog wrapper.
     */
    public static RouteCatalog loadRoutes(String fileName) {
        String path = "routes/" + fileName;
        LOG.info("Loading route catalog from: {}", path);

        Map<String, Object> raw = loadInternal(path, Map.class);
        return new RouteCatalog((Map<String, String>) raw.get("routes"));
    }

    private static <T> T loadInternal(String path, Class<T> type) {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Configuration file not found in classpath: " + path);
            }
            return YamlSupport.create(type).loadAs(is, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration: " + path, e);
        }
    }
}
package dev.realmofevil.automation.engine.bootstrap;

import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.config.SuiteDefinition;
import dev.realmofevil.automation.engine.config.YamlSupport;
import dev.realmofevil.automation.engine.routing.RouteCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central loading point for all YAML configurations.
 * Handles classpath resource resolution, validation, and CLI overrides.
 */
public final class ConfigLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);

    private ConfigLoader() {}

    public static EnvironmentConfig loadEnv(String envName) {
        String path = "env/" + envName + ".yaml";
        validateResourceExists(path, "Environment");
        // LOG.info("Loading environment configuration from: {}", path);
        
        EnvironmentConfig raw = loadInternal(path, EnvironmentConfig.class);

        List<OperatorConfig> mergedOperators = raw.operators().stream()
            .map(op -> mergeDefaults(op, raw.defaults()))
            .map(ConfigLoader::applyOverrides)
            .collect(Collectors.toList());

        return new EnvironmentConfig(raw.name(), raw.defaults(), mergedOperators);
    }

    public static SuiteDefinition loadSuite(String suiteName) {
        String path = "suites/" + suiteName + ".yaml";
        validateResourceExists(path, "Suite");
        // LOG.info("Loading suite definition from: {}", path);
        return loadInternal(path, SuiteDefinition.class);
    }

    @SuppressWarnings("unchecked")
    public static RouteCatalog loadRoutes(String fileName) {
        String path = "routes/" + fileName;
        validateResourceExists(path, "Route Catalog");
        // LOG.debug("Loading route catalog from: {}", path);
        Map<String, Object> raw = loadInternal(path, Map.class);
        Map<String, String> rawRoutes = (Map<String, String>) raw.get("routes");

        if (rawRoutes == null) {
             throw new IllegalStateException("Invalid Route Catalog: '" + path + "'. Missing 'routes' key.");
        }

        return new RouteCatalog(rawRoutes);
    }

    private static void validateResourceExists(String path, String configType) {
        URL resource = ConfigLoader.class.getClassLoader().getResource(path);
        if (resource == null) {
            String msg = String.format(
                "CONFIGURATION ERROR: %s file not found at 'src/main/resources/%s'.\n" +
                "Please check the filename and your classpath.", 
                configType, path
            );
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    private static <T> T loadInternal(String path, Class<T> type) {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(path)) {
            return YamlSupport.create(type).loadAs(is, type);
        } catch (Exception e) {
            String msg = "YAML PARSING ERROR: Failed to parse '" + path + "'. Check syntax indentation and field names.";
            LOG.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    private static OperatorConfig mergeDefaults(OperatorConfig specific, OperatorConfig defaults) {
        if (defaults == null) return specific;
        return new OperatorConfig(
            specific.id(),
            specific.environment(),
            specific.domains(),
            specific.services() != null ? specific.services() : defaults.services(),
            specific.accounts(), // Accounts are usually specific
            specific.databases() != null ? specific.databases() : defaults.databases(),
            specific.rabbit() != null ? specific.rabbit() : defaults.rabbit(),
            specific.routeCatalog() != null ? specific.routeCatalog() : defaults.routeCatalog(),
            specific.parallelism() > 0 ? specific.parallelism() : defaults.parallelism(),
            (specific.auth() != null && !specific.auth().isEmpty()) ? specific.auth() : defaults.auth());
    }

    /**
     * Applies System Property overrides to an OperatorConfig.
     * Pattern: operator.{id}.{property}
     * Example: -Doperator.gmail.parallelism=1
     */
    private static OperatorConfig applyOverrides(OperatorConfig op) {
        String pKey = "operator." + op.id() + ".parallelism";
        int parallelism = Integer.getInteger(pKey, op.parallelism());

        String dKey = "operator." + op.id() + ".domain.desktop";
        String desktop = System.getProperty(dKey);

        OperatorConfig.OperatorDomains domains = op.domains();
        if (desktop != null) {
            domains = new OperatorConfig.OperatorDomains(desktop, op.domains().mobile());
        }

        return new OperatorConfig(
            op.id(),
            op.environment(),
            domains,
            op.services(),
            op.accounts(),
            op.databases(),
            op.rabbit(),
            op.routeCatalog(),
            parallelism,
            op.auth());
    }
}
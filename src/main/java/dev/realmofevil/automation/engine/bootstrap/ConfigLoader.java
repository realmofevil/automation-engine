package dev.realmofevil.automation.engine.bootstrap;

import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.config.SuiteDefinition;
import dev.realmofevil.automation.engine.config.YamlSupport;
import dev.realmofevil.automation.engine.routing.RouteCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.URI;
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
        if (envName == null || envName.isBlank()) {
            throw new IllegalArgumentException("Environment name cannot be null or empty. Provide --env argument.");
        }
        String path = "env/" + envName + ".yaml";
        LOG.info("Loading environment configuration from: {}", path);
        
        EnvironmentConfig raw = loadInternal(path, EnvironmentConfig.class);

        List<OperatorConfig> mergedOperators = raw.operators().stream()
            .map(op -> mergeDefaults(op, raw.defaults()))
            .map(ConfigLoader::applyOverrides)
            .collect(Collectors.toList());

        return new EnvironmentConfig(raw.name(), raw.defaults(), mergedOperators);
    }

    public static SuiteDefinition loadSuite(String suiteName) {
        if (suiteName == null || suiteName.isBlank()) {
            throw new IllegalArgumentException("Suite name cannot be null or empty. Provide --suite argument.");
        }
        String path = "suites/" + suiteName + ".yaml";
        LOG.info("Loading suite definition from: {}", path);
        return loadInternal(path, SuiteDefinition.class);
    }

    @SuppressWarnings("unchecked")
    public static RouteCatalog loadRoutes(String fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("Route catalog filename is null. Check operator config.");
        }
        String path = "routes/" + fileName;
        LOG.debug("Loading route catalog from: {}", path);
        Map<String, Object> raw = loadInternal(path, Map.class);
        Map<String, String> rawRoutes = (Map<String, String>) raw.get("routes");
        return new RouteCatalog(rawRoutes);
        // return new RouteCatalog((Map<String, String>) raw.get("routes"));
    }

    private static <T> T loadInternal(String path, Class<T> type) {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Configuration file not found in classpath: " + path);
            }
            return YamlSupport.create(type).loadAs(is, type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load or parse configuration: " + path, e);
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
            (specific.auth() != null && !specific.auth().isEmpty()) ? specific.auth() : defaults.auth()
        );
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
            op.auth()
        );
    }
}
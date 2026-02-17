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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Central loading point for all YAML configurations.
 * Handles classpath resource resolution, validation, and CLI overrides.
 */
public final class ConfigLoader {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigLoader.class);
    private static final Map<String, Object> SYSTEM_DEFAULTS = Map.of(
            "device", "d",
            "languageId", 2,
            "currencyId", 4,
            "loginType", 1,
            "userAgent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36 OPR/126.0.0.0");

    private ConfigLoader() {}

    public static EnvironmentConfig loadEnv(String envName) {
        if (envName == null || envName.isBlank()) {
            throw new IllegalArgumentException("Environment name cannot be null. Provide --env argument.");
        }
        String path = "env/" + envName + ".yaml";
        validateResourceExists(path, "Environment");
        LOG.info("Loading environment configuration from: {}", path);
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
        LOG.info("Loading suite definition from: {}", path);
        return loadInternal(path, SuiteDefinition.class);
    }

    @SuppressWarnings("unchecked")
    public static RouteCatalog loadRoutes(List<String> fileNames) {
        Map<String, String> mergedRoutes = new HashMap<>();

        if (fileNames == null || fileNames.isEmpty()) {
            return new RouteCatalog(mergedRoutes);
        }

        for (String fileName : fileNames) {
            String path = "routes/" + fileName;
            validateResourceExists(path, "Route File");
            LOG.info("Loading route catalog from: {}", path);
            Map<String, Object> raw = loadInternal(path, Map.class);
            Map<String, String> routes = (Map<String, String>) raw.get("routes");

            if (routes != null) {
                for (String key : routes.keySet()) {
                    if (mergedRoutes.containsKey(key)) {
                        String oldVal = mergedRoutes.get(key);
                        String newVal = routes.get(key);
                        if (!oldVal.equals(newVal)) {
                            LOG.info("Route collision: '{}' in '{}' overrides previous definition. Old: '{}', New: '{}'", key, fileName, oldVal, newVal);
                        } else {
                            LOG.info("Duplicate route key '{}' in '{}' has the same value as previous definition. No override.", key, fileName);
                        }
                    }
                }
                mergedRoutes.putAll(routes);
            }
        }
        if (mergedRoutes.isEmpty()) {
            LOG.warn("No routes were loaded! API tests relying on route keys will fail.");
        } else {
            LOG.info("Loaded {} routes from {} catalogs.", mergedRoutes.size(), fileNames.size());
        }
        return new RouteCatalog(mergedRoutes);
    }

    private static void validateResourceExists(String path, String configType) {
        URL resource = ConfigLoader.class.getClassLoader().getResource(path);
        if (resource == null) {
            String msg = String.format(
                    "CONFIGURATION ERROR: %s file not found at 'src/main/resources/%s'.\n" +
                            "Please check the filename and your classpath.",
                    configType, path);
            LOG.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    private static <T> T loadInternal(String path, Class<T> type) {
        try (InputStream is = ConfigLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null)
                throw new IllegalStateException("Config file not found: " + path);
            return YamlSupport.load(is, type);
        } catch (Exception e) {
            String msg = "YAML PARSING ERROR: Failed to parse '" + path + "'. Check syntax indentation and field names." + e.getMessage();
            LOG.error(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Merges global defaults into specific operator config.
     * Employs strict null and empty collection checks.
     */
    private static OperatorConfig mergeDefaults(OperatorConfig specific, OperatorConfig defaults) {
        Integer finalTenantId = specific.tenantId() != null ? specific.tenantId() : 
                                 (defaults != null ? defaults.tenantId() : null);

        Map<String, Object> mergedContext = new HashMap<>(SYSTEM_DEFAULTS);
        if (defaults != null && defaults.contextDefaults() != null)
            mergedContext.putAll(defaults.contextDefaults());
        if (specific.contextDefaults() != null)
            mergedContext.putAll(specific.contextDefaults());

        List<String> mergedRoutes = new ArrayList<>();
        if (defaults != null && defaults.routeCatalogs() != null)
            mergedRoutes.addAll(defaults.routeCatalogs());
        if (specific.routeCatalogs() != null)
            mergedRoutes.addAll(specific.routeCatalogs());

        int finalParallelism = specific.parallelism() > 0 ? specific.parallelism()
                : (defaults != null && defaults.parallelism() > 0 ? defaults.parallelism() : 1);

        List<OperatorConfig.AuthDefinition> finalAuth = (specific.auth() != null && !specific.auth().isEmpty())
                ? specific.auth()
                : (defaults != null ? defaults.auth() : null);

        return new OperatorConfig(
                specific.id(),
                finalTenantId,
                specific.environment(),
                specific.domains(),
                specific.services() != null ? specific.services() : (defaults != null ? defaults.services() : null),
                specific.accounts(),
                specific.databases() != null ? specific.databases() : (defaults != null ? defaults.databases() : null),
                specific.rabbit() != null ? specific.rabbit() : (defaults != null ? defaults.rabbit() : null),
                mergedContext,
                mergedRoutes,
                finalParallelism,
                finalAuth);
    }

    /**
     * Applies System Property overrides to an OperatorConfig.
     * System Properties:
     *  - operator.{id}.parallelism (int)
     *  - operator.{id}.domain.desktop (String)
     *  - operator.{id}.routeCatalogs (comma-separated String)
     * 
     * Syntax: -Doperator.{id}.{property}=value
     * Example:
     *  -Doperator.gmail.routeCatalogs=custom_routes.yaml,additional_routes.yaml
     *  -Doperator.gmail.domain.desktop=https://custom.desktop.url
     * 
     * Returns a new OperatorConfig instance with overridden values.
     */
    private static OperatorConfig applyOverrides(OperatorConfig op) {
        String prefix = "operator." + op.id();

        int parallelism = Integer.getInteger(prefix + ".parallelism", op.parallelism());

        String pIdOverride = System.getProperty("operator." + op.id() + ".tenantId");
        Integer tenantId = (pIdOverride != null) ? Integer.parseInt(pIdOverride) : op.tenantId();

        String desktop = System.getProperty(prefix + ".domain.desktop");

        OperatorConfig.OperatorDomains domains = op.domains();
        if (desktop != null) {
            LOG.info("Override [{}] Desktop URL: {}", op.id(), desktop);
            domains = new OperatorConfig.OperatorDomains(desktop, op.domains().mobile());
        }

        String routesOverride = System.getProperty(prefix + ".routeCatalogs");
        List<String> routeCatalogs = op.routeCatalogs();
        if (routesOverride != null && !routesOverride.isBlank()) {
            LOG.info("Override [{}] Routes: {}", op.id(), routesOverride);
            routeCatalogs = Arrays.asList(routesOverride.split(","));
        }

        return new OperatorConfig(
                op.id(),
                tenantId,
                op.environment(),
                domains,
                op.services(),
                op.accounts(),
                op.databases(),
                op.rabbit(),
                op.contextDefaults(),
                routeCatalogs,
                parallelism,
                op.auth());
    }
}
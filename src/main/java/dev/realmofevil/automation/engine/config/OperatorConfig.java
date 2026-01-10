package dev.realmofevil.automation.engine.config;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Representation of an Operator configuration in YAML.
 */
public record OperatorConfig(
        String id,
        String environment,
        OperatorDomains domains,
        Map<String, String> services,
        Map<String, ApiAccount> accounts,
        Map<String, DbConfig> databases,
        RabbitConfig rabbit,
        Map<String, Object> contextDefaults,
        String routeCatalog,
        int parallelism,
        List<AuthDefinition> auth) {
    public record OperatorDomains(String desktop, String mobile) {
        public URI desktopUri() {
            return URI.create(desktop);
        }

        public URI mobileUri() {
            return URI.create(mobile);
        }
    }

    public record ApiAccount(Secret username, Secret password, boolean isPool) {
    }

    public record DbConfig(String type, String jdbcUrl, Secret username, Secret password) {
    }

    public record RabbitConfig(
            String host,
            int port,
            String virtualHost,
            Secret username,
            Secret password) {
    }

    public record AuthDefinition(
            AuthType type,
            String loginRoute,
            String credentialField,
            String tokenField,
            TokenSource tokenSource,
            String tokenHeader,
            String useAccount) {
    }

    public enum AuthType {
        BASIC_HEADER, BASIC_URL, LOGIN_TOKEN, SESSION_COOKIE
    }

    public enum TokenSource {
        RESPONSE_BODY, RESPONSE_HEADER
    }

    public String getContextDevice() {
        return String.valueOf(contextDefaults().getOrDefault("device", "d"));
    }

    public int getContextLanguageId() {
        Object val = contextDefaults().getOrDefault("languageId", 2);
        if (val instanceof Integer i) return i;
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (NumberFormatException e) {
            return 2;
        }
    }

    public int getContextCurrencyId() {
        Object val = contextDefaults().getOrDefault("currencyId", 1);
        if (val instanceof Integer i) return i;
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (NumberFormatException e) {
            return 4;
        }
    }

    @Override
    public Map<String, Object> contextDefaults() {
        return contextDefaults != null ? contextDefaults : Collections.emptyMap();
    }

    public URI getServiceUri(String serviceName) {
        if (services == null || !services.containsKey(serviceName)) {
            return domains.desktopUri();
        }
        return URI.create(services.get(serviceName));
    }

    /**
     * Helper to get default database config.
     */
    public DbConfig db() {
        if (databases == null || databases.isEmpty())
            return null;
        return databases.getOrDefault("core", databases.values().iterator().next());
    }
}
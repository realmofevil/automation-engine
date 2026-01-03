package dev.realmofevil.automation.engine.config;

import java.net.URI;
import java.util.List;
import java.util.Map;

public record OperatorConfig(
    String id,
    String environment,
    OperatorDomains domains,
    Map<String, String> services,
    Map<String, ApiAccount> accounts,
    Map<String, DbConfig> databases,
    RabbitConfig rabbit,
    String routeCatalog,
    int parallelism,
    List<AuthDefinition> auth
) {
    public record OperatorDomains(String desktop, String mobile) {
        public URI desktopUri() { return URI.create(desktop); }
        public URI mobileUri() { return URI.create(mobile); }
    }

    public record ApiAccount(Secret username, Secret password, boolean isPool) {}

    public record DbConfig(String type, String jdbcUrl, Secret username, Secret password) {}

    public record RabbitConfig(
        String host, 
        int port, 
        String virtualHost, 
        Secret username, 
        Secret password
    ) {}

    public record AuthDefinition(
        AuthType type, 
        String loginRoute, 
        String tokenField, // JSON Path (e.g. "data.token") OR Header Name (e.g. "Set-Cookie" or "SessionID")
        TokenSource tokenSource, // ENUM: RESPONSE_BODY or RESPONSE_HEADER
        String tokenHeader,
        
        // NEW: Allows this specific auth step to force a specific account alias
        // e.g. "proxy_admin" for Basic Auth, while the test uses "customer_1"
        String useAccount 
    ) {}
    
    public enum AuthType {
        BASIC_HEADER,
        BASIC_URL,
        LOGIN_TOKEN,
        SESSION_COOKIE
    }

    public enum TokenSource {
        RESPONSE_BODY,
        RESPONSE_HEADER
    }

    public URI getServiceUri(String serviceName) {
        if (services == null || !services.containsKey(serviceName)) {
            return domains.desktopUri();
        }
        return URI.create(services.get(serviceName));
    }
}
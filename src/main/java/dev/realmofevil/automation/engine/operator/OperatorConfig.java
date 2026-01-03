package dev.realmofevil.automation.engine.operator;

import dev.realmofevil.automation.engine.auth.AuthenticationChain;
import dev.realmofevil.automation.engine.db.DbConfig;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public final class OperatorConfig {

    private final OperatorId id;
    private final OperatorDomains domains;
    private final URI desktopDomain;
    private final URI mobileDomain;
    private final int threads;
    private final String routeCatalog;
    private final Map<String, ApiAccount> apiAccounts;
    private final DbConfig dbConfig;

    public OperatorConfig(
            OperatorId id,
            OperatorDomains domains,
            URI desktopDomain,
            URI mobileDomain,
            int threads,
            String routeCatalog,
            Map<String, ApiAccount> apiAccounts,
            DbConfig dbConfig) {
        this.id = Objects.requireNonNull(id);
        this.domains = Objects.requireNonNull(domains);
        this.desktopDomain = Objects.requireNonNull(desktopDomain);
        this.mobileDomain = Objects.requireNonNull(mobileDomain);
        this.threads = threads;
        this.routeCatalog = Objects.requireNonNull(routeCatalog);
        this.apiAccounts = Map.copyOf(apiAccounts);
        this.dbConfig = Objects.requireNonNull(dbConfig);
    }

    public OperatorId id() {
        return id;
    }

    public OperatorDomains domains() {
        return domains;
    }

    public URI desktopDomain() {
        return desktopDomain;
    }

    public URI mobileDomain() {
        return mobileDomain;
    }

    public int threads() {
        return threads;
    }

    public String routeCatalog() {
        return routeCatalog;
    }

    public Map<String, ApiAccount> apiAccounts() {
        return apiAccounts;
    }

    public DbConfig dbConfig() {
        return dbConfig;
    }
}
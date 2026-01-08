package dev.realmofevil.automation.engine.context;

import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.http.ApiClient;
import dev.realmofevil.automation.engine.auth.AuthSession;
import dev.realmofevil.automation.engine.auth.AuthenticationChain;
import dev.realmofevil.automation.engine.auth.BasicAuthenticationStep;
import dev.realmofevil.automation.engine.auth.SessionAuthenticationStep;
import dev.realmofevil.automation.engine.auth.TokenAuthenticationStep;
import dev.realmofevil.automation.engine.auth.AccountPool;
import dev.realmofevil.automation.engine.auth.AuthManager;
import dev.realmofevil.automation.engine.routing.RouteCatalog;
import dev.realmofevil.automation.engine.db.DbClient;
import dev.realmofevil.automation.engine.db.TransactionManager;
import dev.realmofevil.automation.engine.messaging.MessagingPort;
import dev.realmofevil.automation.engine.messaging.RabbitMqAdapter;

import javax.sql.DataSource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExecutionContext {
    private final OperatorConfig operatorConfig;
    private final RouteCatalog routeCatalog;
    private final ApiClient apiClient;
    private final AuthSession authSession;
    private final AuthManager authManager;
    private final Map<String, TransactionManager> txManagers;
    private final Map<String, DbClient> dbClients;
    private final AccountPool accountPool;
    private OperatorConfig.ApiAccount leasedAccount;
    private final MessagingPort messagingPort;
    private final AuthenticationChain authenticationChain;
    private final Map<String, Object> state = new ConcurrentHashMap<>();

    public ExecutionContext(OperatorConfig config, RouteCatalog catalog, Map<String, DataSource> dataSources,
            AccountPool pool) {
        this.operatorConfig = config;
        this.routeCatalog = catalog;

        this.authenticationChain = new AuthenticationChain(List.of(
                new BasicAuthenticationStep(),
                new TokenAuthenticationStep(),
                new SessionAuthenticationStep()));
        this.authSession = new AuthSession();
        this.authManager = new AuthManager(this);
        this.apiClient = new ApiClient(this);

        this.messagingPort = new RabbitMqAdapter(config.rabbit(), apiClient.getMapper());

        this.txManagers = new ConcurrentHashMap<>();
        this.dbClients = new ConcurrentHashMap<>();

        dataSources.forEach((key, ds) -> {
            TransactionManager tm = new TransactionManager(ds);
            txManagers.put(key, tm);
            dbClients.put(key, new DbClient(tm));
        });

        this.accountPool = pool;
    }

    public OperatorConfig config() {
        return operatorConfig;
    }

    public RouteCatalog routes() {
        return routeCatalog;
    }

    public ApiClient api() {
        return apiClient;
    }

    public MessagingPort messaging() {
        return messagingPort;
    }

    public AuthSession auth() {
        return authSession;
    }

    public AuthManager authManager() {
        return authManager;
    }

    public AccountPool getAccountPool() {
        return accountPool;
    }

    public AuthenticationChain authChain() {
        return authenticationChain;
    }

    public Map<String, Object> state() {
        return state;
    }

    public void setLeasedAccount(OperatorConfig.ApiAccount acc) {
        this.leasedAccount = acc;
    }

    public OperatorConfig.ApiAccount getLeasedAccount() {
        return leasedAccount;
    }

    public DbClient db() {
        return db("core");
    }

    public DbClient db(String name) {
        if (!dbClients.containsKey(name)) {
            if (dbClients.size() == 1)
                return dbClients.values().iterator().next();
            throw new IllegalArgumentException("Database '" + name + "' not configured for this operator.");
        }
        return dbClients.get(name);
    }

    public TransactionManager transactions(String name) {
        return txManagers.get(name);
    }

    public Map<String, TransactionManager> getAllTransactionManagers() {
        return txManagers;
    }

    public void closeResources() {
        if (messagingPort != null) {
            messagingPort.close();
        }
    }
}
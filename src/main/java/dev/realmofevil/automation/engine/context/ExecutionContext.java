package dev.realmofevil.automation.engine.context;

import dev.realmofevil.automation.engine.auth.AccountPool;
import dev.realmofevil.automation.engine.auth.AuthenticationChain;
import dev.realmofevil.automation.engine.db.OperatorDbPool;
import dev.realmofevil.automation.engine.db.TransactionManager;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import dev.realmofevil.automation.engine.operator.OperatorConfig;
import dev.realmofevil.automation.engine.reporting.ReportingContext;
import dev.realmofevil.automation.engine.routing.RouteCatalog;

import java.net.http.HttpClient;
import java.time.Clock;

public final class ExecutionContext {

    private final OperatorConfig operator;
    private final RouteCatalog routes;
    private final HttpClient httpClient;
    private final Clock clock;
    private final AccountPool accounts;
    private final AuthenticationChain auth;
    private final TransactionManager transactions;
    private final ReportingContext reportingContext;

    public ExecutionContext(
            OperatorConfig operator,
            RouteCatalog routes,
            HttpClient httpClient,
            Clock clock,
            OperatorDbPool dbPool,
            OperatorExecutionPlan plan
    ) {
        this.operator = operator;
        this.routes = routes;
        this.httpClient = httpClient;
        this.clock = clock;
        this.accounts = operator.accountPool();
        this.auth = operator.authenticationChain();
        this.transactions = new TransactionManager(dbPool);
        this.reportingContext = new ReportingContext(
                plan.environment(),
                plan.operator().id(),
                plan.suite().name(),
                plan.executionId()
        );
    }

    public OperatorConfig operator() {
        return operator;
    }

    public RouteCatalog routes() {
        return routes;
    }

    public HttpClient httpClient() {
        return httpClient;
    }

    public Clock clock() {
        return clock;
    }

    public AccountPool accounts() {
        return accounts;
    }

    public AuthenticationChain auth() {
        return auth;
    }

    public TransactionManager transactions() {
        return transactions;
    }
}
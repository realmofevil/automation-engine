package dev.realmofevil.automation.engine.execution;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.operator.OperatorConfig;
import dev.realmofevil.automation.engine.routing.RouteCatalog;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class OperatorRuntimeResources implements AutoCloseable {

    private final ExecutorService executor;
    private final ExecutionContext context;

    public OperatorRuntimeResources(
            OperatorConfig operator,
            RouteCatalog routes
    ) {
        this.executor = Executors.newFixedThreadPool(operator.threads());

        HttpClient httpClient = HttpClient.newBuilder()
                .executor(executor)
                .build();

        this.context = new ExecutionContext(
                operator,
                routes,
                httpClient,
                Clock.systemUTC()
        );
    }

    public ExecutionContext context() {
        return context;
    }

    public ExecutorService executor() {
        return executor;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
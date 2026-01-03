package dev.realmofevil.automation.engine.execution;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.operator.OperatorConfig;
import dev.realmofevil.automation.engine.reporting.AllureBootstrap;
import dev.realmofevil.automation.engine.reporting.ReportingContext;
import dev.realmofevil.automation.engine.reporting.TestReporter;
import dev.realmofevil.automation.engine.routing.RouteCatalog;

import java.net.http.HttpClient;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class OperatorRuntime implements AutoCloseable {

    private final OperatorConfig operator;
    private final ExecutorService executor;
    private final ExecutionContext context;

    public OperatorRuntime(
            OperatorConfig operator,
            RouteCatalog routes
    ) {
        this.operator = operator;
        this.executor = Executors.newFixedThreadPool(operator.threads());

        HttpClient httpClient = HttpClient.newBuilder()
                .executor(executor)
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        this.context = new ExecutionContext(
                operator,
                routes,
                httpClient,
                Clock.systemUTC()
        );
    }

    public OperatorConfig operator() {
        return operator;
    }

    public ExecutionContext context() {
        return context;
    }

    public ExecutorService executor() {

        // AllureBootstrap.apply(context.reportingContext());
        // TestReporter.info("Starting execution: " +
        //     context.reportingContext().executionId());

        return executor;
    }

    @Override
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
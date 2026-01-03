// TODO: research if JUnit Launcher can natively support parallel execution with controlled thread pool
// https://junit.org/junit5/docs/current/user-guide/#launcher-execution
// https://junit.org/junit5/docs/current/user-guide/#launcher-api
// https://junit.org/junit5/docs/current/user-guide/#extensions-listeners
// https://www.baeldung.com/junit-5-listener-extensions
// Research if we can use ExecutorService to manage parallel execution and iterate operators or single operator requested

package dev.realmofevil.automation.engine.executor;

import dev.realmofevil.automation.engine.auth.AuthManager;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.db.DbContext;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.engine.discovery.DiscoverySelectors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executes JUnit tests for a single operator with controlled parallelism.
 *
 * Responsibilities:
 * - Bind ExecutionContext per operator
 * - Manage authentication lifecycle
 * - Manage DB lifecycle
 * - Execute JUnit Platform with executor
 */
public final class OperatorTestExecutor {

    private OperatorTestExecutor() {}

    public static void execute(OperatorExecutionPlan plan) {

        ExecutorService executor = Executors.newFixedThreadPool(plan.endpoint().parallelism());

        try {
            ExecutionContext.init(plan.environment(), plan.operator());

            AuthManager.acquire();

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(
                            DiscoverySelectors.selectPackage("tests"))
                    .filters(plan.tagFilter())
                    .build();

            Launcher launcher = LauncherFactory.create();
            launcher.execute(request, executor); // launcher.execute(request, new ExecutionListener(), executor);

        } finally {
            AuthManager.release();
            DbContext.close();
            ExecutionContext.clear();
            executor.shutdown();
        }
    }
}

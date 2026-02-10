package dev.realmofevil.automation.engine.junit;

import dev.realmofevil.automation.engine.auth.AccountPool;
import dev.realmofevil.automation.engine.auth.annotations.Public;
import dev.realmofevil.automation.engine.auth.annotations.UseAccount;
import dev.realmofevil.automation.engine.bootstrap.ConfigLoader;
import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.config.SuiteDefinition;
import dev.realmofevil.automation.engine.context.ContextHolder;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.db.DataSourceFactory;
import dev.realmofevil.automation.engine.db.TransactionManager;
import dev.realmofevil.automation.engine.db.annotations.CommitTransaction;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import dev.realmofevil.automation.engine.reporting.StepReporter;
import dev.realmofevil.automation.engine.routing.RouteCatalog;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DynamicOperatorTestFactory {

    private DynamicOperatorTestFactory() {
    }

    public static DynamicContainer create(OperatorExecutionPlan plan, String methodFilter) {
        try {
            OperatorConfig op = plan.operator();
            RouteCatalog routes = ConfigLoader.loadRoutes(op.routeCatalogs());
            AccountPool accountPool = new AccountPool(op.accounts());

            Map<String, DataSource> dataSources = new HashMap<>();
            if (op.databases() != null) {
                op.databases().forEach((key, dbConfig) -> {
                    try {
                        dataSources.put(key, DataSourceFactory.create(dbConfig));
                    } catch (Exception e) {
                        StepReporter.warn("Skipping DB initialization '" + key + "': " + e.getMessage());
                    }
                });
            }

            List<DynamicContainer> classContainers = new ArrayList<>();
            for (SuiteDefinition.TestEntry entry : plan.tests()) {
                classContainers.add(createClassContainer(op, routes, dataSources, accountPool, entry, methodFilter));
            }
            return DynamicContainer.dynamicContainer(op.id(), classContainers);

        } catch (Exception e) {
            StepReporter.error("Failed to initialize Operator Container [" + plan.operator().id() + "]", e);
            throw new RuntimeException("Operator Initialization Failed: " + e.getMessage());
        }
    }

    private static DynamicContainer createClassContainer(
            OperatorConfig op,
            RouteCatalog routes,
            Map<String, DataSource> dataSources,
            AccountPool accountPool,
            SuiteDefinition.TestEntry entry,
            String methodFilter) {
        try {
            Class<?> testClass = Class.forName(entry.className());
            List<DynamicTest> tests = new ArrayList<>();

            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
                    if (methodFilter != null && !methodFilter.isBlank() && !method.getName().equals(methodFilter)) {
                        continue;
                    }
                    tests.add(DynamicTest.dynamicTest(
                            method.getName(),
                            () -> executeTestLifecycle(op, routes, dataSources, accountPool, testClass, method)));
                }
            }
            return DynamicContainer.dynamicContainer(testClass.getSimpleName(), tests);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Test class not found: " + entry.className(), e);
        }
    }

    private static void executeTestLifecycle(
            OperatorConfig op,
            RouteCatalog routes,
            Map<String, DataSource> dataSources,
            AccountPool accountPool,
            Class<?> testClass,
            Method testMethod) throws Throwable {

        Flaky flaky = testMethod.getAnnotation(Flaky.class);
        int maxRetries = (flaky != null) ? flaky.retries() : 0;
        int attempt = 0;
        Throwable lastError = null;

        while (attempt <= maxRetries) {
            try {
                runSingleExecution(op, routes, dataSources, accountPool, testClass, testMethod, attempt);
                return;
            } catch (Throwable e) {
                lastError = e;
                attempt++;
                if (attempt <= maxRetries) {
                    StepReporter.warn("Test failed (Attempt " + attempt + "/" + (maxRetries + 1)
                            + "). Retrying due to @Flaky...");
                }
            }
        }
        if (lastError instanceof InvocationTargetException ie) {
            throw ie.getTargetException();
        }
        throw lastError;
    }

    private static void runSingleExecution(
            OperatorConfig op,
            RouteCatalog routes,
            Map<String, DataSource> dataSources,
            AccountPool accountPool,
            Class<?> testClass,
            Method testMethod,
            int attempt) throws Throwable {

        ExecutionContext ctx = new ExecutionContext(op, routes, dataSources, accountPool);
        ContextHolder.set(ctx);
        Object testInstance = testClass.getDeclaredConstructor().newInstance();
        String testName = testClass.getSimpleName() + "." + testMethod.getName();

        try {
            Allure.label("operator", op.id());
            Allure.label("environment", op.environment());
            Allure.label("parentSuite", op.id());
            Allure.label("suite", testClass.getSimpleName());

            if (attempt == 0) {
                StepReporter.info(">>> START TEST: [" + op.id() + "] " + testName);
            } else {
                StepReporter.warn(">>> RETRY ATTEMPT #" + attempt + " FOR TEST: [" + op.id() + "] " + testName);
            }

            ctx.getAllTransactionManagers().values().forEach(TransactionManager::begin);

            boolean isPublic = testMethod.isAnnotationPresent(Public.class)
                    || testClass.isAnnotationPresent(Public.class);
            if (!isPublic) {
                UseAccount annotation = testMethod.getAnnotation(UseAccount.class);
                String requestedAlias = (annotation != null) ? annotation.id() : null;
                ctx.authManager().acquireAccount(requestedAlias);
            } else {
                ctx.authManager().applyTransportAuthOnly();
            }
            invokeLifecycleMethods(testClass, testInstance, BeforeEach.class);
            testMethod.setAccessible(true);
            testMethod.invoke(testInstance);
            invokeLifecycleMethods(testClass, testInstance, AfterEach.class);

            boolean shouldCommit = testMethod.isAnnotationPresent(CommitTransaction.class);
            ctx.getAllTransactionManagers().values().forEach(tm -> tm.end(shouldCommit));

        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            StepReporter.error("!!! TEST FAILED: [" + op.id() + "] " + testName + " | " + cause.getMessage(), cause);

            ctx.getAllTransactionManagers().values().forEach(tm -> {
                try {
                    tm.end(false);
                } catch (Exception ignored) {
                }
            });
            throw cause;
        } catch (Throwable e) {
            StepReporter.error("!!! TEST ERROR: [" + op.id() + "] " + testName + " | " + e.getMessage(), e);
            ctx.getAllTransactionManagers().values().forEach(tm -> {
                try {
                    tm.end(false);
                } catch (Exception ignored) {
                }
            });
            throw e;
        } finally {
            ctx.authManager().releaseAccount();
            ctx.closeResources();
            ContextHolder.clear();
            StepReporter.info("<<< END TEST:   [" + op.id() + "] " + testName);
        }
    }

    private static void invokeLifecycleMethods(Class<?> clazz, Object instance,
            Class<? extends java.lang.annotation.Annotation> annotation) throws Exception {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(annotation)) {
                m.setAccessible(true);
                m.invoke(instance);
            }
        }
    }
}
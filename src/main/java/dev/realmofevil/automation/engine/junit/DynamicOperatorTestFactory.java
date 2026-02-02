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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class DynamicOperatorTestFactory {

    private DynamicOperatorTestFactory() {}

    public static DynamicContainer create(OperatorExecutionPlan plan) {
        OperatorConfig op = plan.operator();

        RouteCatalog routes = ConfigLoader.loadRoutes(op.routeCatalogs());

        DataSource ds = DataSourceFactory.create(op.db());
        Map<String, DataSource> dataSources = Collections.singletonMap("core", ds);

        AccountPool accountPool = new AccountPool(op.accounts());

        List<DynamicContainer> classContainers = new ArrayList<>();
        for (SuiteDefinition.TestEntry entry : plan.tests()) {
            classContainers.add(createClassContainer(op, routes, dataSources, accountPool, entry));
        }

        return DynamicContainer.dynamicContainer(op.id(), classContainers);
    }

    private static DynamicContainer createClassContainer(
            OperatorConfig op,
            RouteCatalog routes,
            Map<String, DataSource> dataSources,
            AccountPool accountPool,
            SuiteDefinition.TestEntry entry) {
        try {
            Class<?> testClass = Class.forName(entry.className());
            List<DynamicTest> tests = new ArrayList<>();

            for (Method method : testClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
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
            Method testMethod) throws Exception {

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

        if (lastError instanceof Exception)
            throw (Exception) lastError;
        throw new RuntimeException(lastError);
    }

    private static void runSingleExecution(
            OperatorConfig op,
            RouteCatalog routes,
            Map<String, DataSource> dataSources,
            AccountPool accountPool,
            Class<?> testClass,
            Method testMethod,
            int attempt) throws Exception {

        ExecutionContext ctx = new ExecutionContext(op, routes, dataSources, accountPool);
        ContextHolder.set(ctx);
        TransactionManager txManager = ctx.transactions("core");

        Object testInstance = testClass.getDeclaredConstructor().newInstance();
        String testName = testClass.getSimpleName() + "." + testMethod.getName();

        try {
            Allure.label("operator", op.id());
            Allure.label("environment", op.environment());
            Allure.label("parentSuite", op.id());
            Allure.label("suite", testClass.getSimpleName());

            if (attempt > 0) {
                StepReporter.warn(">>> START RETRY " + attempt + ": [" + op.id() + "] " + testName);
            } else {
                StepReporter.info(">>> START TEST: [" + op.id() + "] " + testName);
            }

            if (txManager != null)
                txManager.begin();

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
            if (txManager != null)
                txManager.end(shouldCommit);

        } catch (Exception e) {
            StepReporter.error("!!! TEST FAILED: [" + op.id() + "] " + testName, e);

            if (txManager != null) {
                try {
                    txManager.end(false);
                } catch (Exception ignored) {
                }
            }

            if (e instanceof java.lang.reflect.InvocationTargetException) {
                throw (Exception) e.getCause();
            }
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
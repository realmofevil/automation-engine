package dev.realmofevil.automation.engine.junit;

import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import dev.realmofevil.automation.engine.execution.OperatorExecutionSession;
import dev.realmofevil.automation.engine.execution.OperatorRuntime;
import dev.realmofevil.automation.engine.suite.TestDefinition;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public final class DynamicOperatorTestFactory {

    private DynamicOperatorTestFactory() {}

    public static DynamicContainer create(
            OperatorExecutionPlan plan
    ) {
        OperatorRuntime runtime =
                new OperatorRuntime(
                        plan.operator(),
                        plan.routeCatalog()
                );

        OperatorExecutionSession session =
                new OperatorExecutionSession(runtime);

        return dynamicContainer(
                "Operator: " + plan.operator().id(),
                plan.tests().stream()
                        .map(test -> createTest(test, session))
                        .toList()
        );
    }

    private static DynamicTest createTest(
            TestDefinition test,
            OperatorExecutionSession session
    ) {
        return dynamicTest(
                test.className(),
                () -> runTestClass(test, session)
        );
    }

    private static void runTestClass(
            TestDefinition test,
            OperatorExecutionSession session
    ) throws Exception {

        session.beforeTest();
        try {
            Class<?> clazz = Class.forName(test.className());
            Object instance = clazz.getDeclaredConstructor().newInstance();

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(
                        org.junit.jupiter.api.Test.class
                )) {
                    method.setAccessible(true);
                    method.invoke(instance);
                }
            }
        } finally {
            session.afterTest();
        }
    }
}

/** old version
package dev.realmofevil.automation.engine.junit;

import dev.realmofevil.automation.engine.context.ExecutionContextHolder;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import dev.realmofevil.automation.engine.execution.OperatorRuntimeResources;
import dev.realmofevil.automation.engine.suite.TestDefinition;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public final class DynamicOperatorTestFactory {

    private DynamicOperatorTestFactory() {}

    public static DynamicContainer create(
            OperatorExecutionPlan plan
    ) {
        return dynamicContainer(
                "Operator: " + plan.operator().id(),
                executePlan(plan)
        );
    }

    private static List<DynamicTest> executePlan(
            OperatorExecutionPlan plan
    ) {
        OperatorRuntimeResources resources =
                new OperatorRuntimeResources(
                        plan.operator(),
                        plan.routeCatalog()
                );

        return plan.tests().stream()
                .map(test -> dynamicTest(
                        test.className(),
                        () -> runTestClass(test, resources)
                ))
                .toList();
    }

    private static void runTestClass(
            TestDefinition test,
            OperatorRuntimeResources resources
    ) throws Exception {

        ExecutionContextHolder.set(resources.context());
        try {
            Class<?> clazz = Class.forName(test.className());
            Object testInstance = clazz.getDeclaredConstructor().newInstance();

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(
                        org.junit.jupiter.api.Test.class
                )) {
                    method.setAccessible(true);
                    method.invoke(testInstance);
                }
            }
        } finally {
            ExecutionContextHolder.clear();
            resources.close();
        }
    }
}
**/
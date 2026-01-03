package dev.realmofevil.automation.engine.context;

import dev.realmofevil.automation.engine.reporting.TestReporter;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;

public final class ExecutionContextBootstrap {

    public static ExecutionContext bootstrap(OperatorExecutionPlan plan) {

        TestReporter.info("Bootstrapping execution context");
        TestReporter.info("Environment: " + plan.environment());
        TestReporter.info("Operator: " + plan.operator().id());

        return new ExecutionContext(plan);
    }
}
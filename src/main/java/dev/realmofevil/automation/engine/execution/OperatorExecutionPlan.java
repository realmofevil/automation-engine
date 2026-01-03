package dev.realmofevil.automation.engine.execution;

import dev.realmofevil.automation.engine.operator.OperatorConfig;
import dev.realmofevil.automation.engine.routing.RouteCatalog;
import dev.realmofevil.automation.engine.suite.TestDefinition;

import java.util.List;
import java.util.Objects;

public final class OperatorExecutionPlan {

    private final OperatorConfig operator;
    private final RouteCatalog routeCatalog;
    private final List<TestDefinition> tests;

    public OperatorExecutionPlan(
            OperatorConfig operator,
            RouteCatalog routeCatalog,
            List<TestDefinition> tests
    ) {
        this.operator = Objects.requireNonNull(operator);
        this.routeCatalog = Objects.requireNonNull(routeCatalog);
        this.tests = List.copyOf(tests);
    }

    public OperatorConfig operator() {
        return operator;
    }

    public RouteCatalog routeCatalog() {
        return routeCatalog;
    }

    public List<TestDefinition> tests() {
        return tests;
    }

    @Override
    public String toString() {
        return "OperatorExecutionPlan{" +
                "operator=" + operator.id() +
                ", tests=" + tests.size() +
                '}';
    }
}
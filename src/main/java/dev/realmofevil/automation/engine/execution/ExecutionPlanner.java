package dev.realmofevil.automation.engine.execution;

import dev.realmofevil.automation.engine.bootstrap.CliOverrides;
import dev.realmofevil.automation.engine.operator.OperatorConfig;
import dev.realmofevil.automation.engine.routing.RouteCatalog;
import dev.realmofevil.automation.engine.routing.RouteCatalogLoader;
import dev.realmofevil.automation.engine.suite.SuiteDefinition;
import dev.realmofevil.automation.engine.suite.TestDefinition;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ExecutionPlanner {

    private ExecutionPlanner() {}

    public static List<OperatorExecutionPlan> plan(
            List<OperatorConfig> operators,
            SuiteDefinition suite,
            CliOverrides overrides
    ) {

        Set<String> includedOperators = resolveOperators(suite, overrides);

        return operators.stream()
                .filter(op -> includedOperators.contains(op.id()))
                .map(op -> buildPlan(op, suite))
                .toList();
    }

    private static OperatorExecutionPlan buildPlan(
            OperatorConfig operator,
            SuiteDefinition suite
    ) {
        RouteCatalog routes =
                RouteCatalogLoader.load(operator.routeCatalog());

        return new OperatorExecutionPlan(
                operator,
                routes,
                suite.tests()
        );
    }

    private static Set<String> resolveOperators(
            SuiteDefinition suite,
            CliOverrides overrides
    ) {
        if (overrides.has("operator")) {
            return Set.of(overrides.get("operator").split(","));
        }
        return suite.operators().stream().collect(Collectors.toSet());
    }
}
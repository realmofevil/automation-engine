package dev.realmofevil.automation.engine.execution;

import dev.realmofevil.automation.engine.config.OperatorResolver;
import dev.realmofevil.automation.engine.context.ExecutionContext;

import java.util.List;

public final class OperatorExecutionPlanner {

    private OperatorExecutionPlanner() {
    }

    public static List<OperatorExecutionPlan> plan() {
        var env = ExecutionContext.get().environment();

        return OperatorResolver.resolveOperators()
                .stream()
                .map(op -> new OperatorExecutionPlan(
                        op,
                        env.operators().get(op)))
                .toList();
    }
}

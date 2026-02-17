package dev.realmofevil.automation.engine.execution;

import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.config.SuiteDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;

public final class ExecutionPlanner {
    private static final Logger LOG = LoggerFactory.getLogger(ExecutionPlanner.class);

    private ExecutionPlanner() {}

    public static List<OperatorExecutionPlan> plan(List<OperatorConfig> operators, SuiteDefinition suite) {
        List<OperatorExecutionPlan> plans = new ArrayList<>();
        List<String> targets = suite.targetOperators();

        boolean runAll = targets.stream().anyMatch(t -> t.equalsIgnoreCase("ALL"));

        for (OperatorConfig op : operators) {
            boolean isTargeted = runAll || targets.stream().anyMatch(t -> t.equalsIgnoreCase(op.id()));

            if (isTargeted) {
                LOG.info("Scheduling suite '{}' for operator '{}' ({})", suite.name(), op.id(), op.environment());
                plans.add(new OperatorExecutionPlan(op, suite.tests()));
            } else {
                LOG.info("Skipping operator '{}' (not in target list)", op.id());
            }
        }

        if (plans.isEmpty()) {
            throw new IllegalStateException("No operators matched the suite targets: " + targets);
        }

        return plans;
    }
}
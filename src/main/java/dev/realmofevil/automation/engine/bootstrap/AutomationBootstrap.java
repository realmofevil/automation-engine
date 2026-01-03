package dev.realmofevil.automation.engine.bootstrap;

import dev.realmofevil.automation.engine.execution.ExecutionPlanner;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import dev.realmofevil.automation.engine.operator.OperatorConfig;
import dev.realmofevil.automation.engine.suite.SuiteDefinition;
import dev.realmofevil.automation.engine.suite.SuiteLoader;

import java.util.List;

public final class AutomationBootstrap {

    private AutomationBootstrap() {}

    public static void main(String[] args) {
        CliArguments cliArgs = CliArguments.parse(args);
        CliOverrides overrides = CliOverridesFactory.from(cliArgs);

        String env = cliArgs.get("env");
        String suiteFile = cliArgs.get("suite");

        if (env == null || suiteFile == null) {
            throw new IllegalArgumentException(
                    "Both --env and --suite must be provided"
            );
        }

        List<OperatorConfig> operators =
                OperatorConfigLoader.load("env/" + env + ".yaml", overrides);

        SuiteDefinition suite = SuiteLoader.load(suiteFile);

        List<OperatorExecutionPlan> plans =
                ExecutionPlanner.plan(operators, suite, overrides);

        plans.forEach(plan ->
                System.out.println("Prepared execution plan: " + plan)
        );
    }
}
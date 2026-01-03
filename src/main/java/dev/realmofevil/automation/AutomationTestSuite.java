package dev.realmofevil.automation;

import dev.realmofevil.automation.engine.bootstrap.*;
import dev.realmofevil.automation.engine.execution.ExecutionPlanner;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import dev.realmofevil.automation.engine.junit.DynamicOperatorTestFactory;
import dev.realmofevil.automation.engine.operator.OperatorConfig;
import dev.realmofevil.automation.engine.suite.SuiteDefinition;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

import java.util.List;

public final class AutomationTestSuite {

    @TestFactory
    List<DynamicContainer> runSuite() {

        CliArguments cli =
                CliArguments.parse(System.getProperty("cli", "")
                        .isBlank()
                        ? new String[0]
                        : System.getProperty("cli").split(" "));

        CliOverrides overrides =
                CliOverridesFactory.from(cli);

        String env = cli.get("env");
        String suiteFile = cli.get("suite");

        if (env == null || suiteFile == null) {
            throw new IllegalStateException(
                    "Missing --env or --suite"
            );
        }

        List<OperatorConfig> operators =
                OperatorConfigLoader.load(
                        "env/" + env + ".yaml",
                        overrides
                );

        SuiteDefinition suite =
                SuiteLoader.load(suiteFile);

        List<OperatorExecutionPlan> plans =
                ExecutionPlanner.plan(
                        operators,
                        suite,
                        overrides
                );

        return plans.stream()
                .map(DynamicOperatorTestFactory::create)
                .toList();
    }
}
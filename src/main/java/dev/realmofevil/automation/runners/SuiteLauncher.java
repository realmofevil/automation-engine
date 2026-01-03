package dev.realmofevil.automation.runners;

import dev.realmofevil.automation.engine.config.CliOverrides;
import dev.realmofevil.automation.engine.executor.OperatorTestExecutor;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlanner;
import dev.realmofevil.automation.engine.reporting.AllureBootstrap;
import dev.realmofevil.automation.engine.reporting.AllureEnvironmentWriter;
import dev.realmofevil.automation.engine.reporting.AllureHistoryManager;
import dev.realmofevil.automation.engine.suite.SuiteConfigLoader;

import java.util.Map;

/**
 * Entry-point for suite-based execution (CLI / CI) and overrides.
 */
public class SuiteLauncher {

    public static void main(String[] args) {

        // var overrides = CliOverrides.parse(args);
        Map<String, String> overrides = CliOverrides.parse(args);

        SuiteConfigLoader.load(overrides);

        AllureBootstrap.init();
        AllureEnvironmentWriter.write();
        AllureHistoryManager.restoreHistory();

        for (OperatorExecutionPlan plan : OperatorExecutionPlanner.plan()) {
            OperatorTestExecutor.execute(plan);
        }

        AllureHistoryManager.persistHistory();
    }
}

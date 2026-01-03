package dev.realmofevil.automation.engine.junit;

import dev.realmofevil.automation.engine.bootstrap.ConfigLoader;
import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.config.SuiteDefinition;
import dev.realmofevil.automation.engine.execution.ExecutionPlanner;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

/**
 * The main entry point for the test execution.
 * To run: mvn test -Dtest=AutomationTestSuite -Denv=dev -Dsuite=smoke
 */
public class AutomationTestSuite {

    private static final Logger LOG = LoggerFactory.getLogger(AutomationTestSuite.class);

    @TestFactory
    public Stream<DynamicContainer> execute() {

        String envName = System.getProperty("env", "dev");
        String suiteName = System.getProperty("suite", "smoke");

        LOG.info("Starting Automation Engine. Environment: {}, Suite: {}", envName, suiteName);

        EnvironmentConfig envConfig = ConfigLoader.loadEnv(envName);
        SuiteDefinition suiteDef = ConfigLoader.loadSuite(suiteName);

        List<OperatorExecutionPlan> plans = ExecutionPlanner.plan(envConfig.operators(), suiteDef);

        return plans.stream()
                .map(DynamicOperatorTestFactory::create);
    }
}
package dev.realmofevil.automation.engine.junit;

import dev.realmofevil.automation.engine.bootstrap.ConfigLoader;
import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.config.SuiteDefinition;
import dev.realmofevil.automation.engine.execution.ExecutionPlanner;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import dev.realmofevil.automation.engine.reporting.StepReporter;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * The main entry point for the test execution.
 * To run a full suite: mvn test -Dtest=AutomationTestSuite -Denv=dev -Dsuite=smoke
 * To run a single class: mvn test -Dtest=AutomationTestSuite -Denv=dev -Dtest.class=dev.realmofevil.automation.tests.user.LoginTest
 */
public class AutomationTestSuite {

    private static final Logger LOG = LoggerFactory.getLogger(AutomationTestSuite.class);

    @TestFactory
    public Stream<DynamicContainer> execute() {

        String envName = System.getProperty("env");
        String suiteName = System.getProperty("suite");
        String singleTestClass = System.getProperty("test.class");

        validateContext(envName, suiteName, singleTestClass);

        LOG.info("Starting Automation Engine. Environment: {}, Suite: {}", envName, suiteName);

        EnvironmentConfig envConfig = ConfigLoader.loadEnv(envName);
        SuiteDefinition suiteDef;

        if (singleTestClass != null && !singleTestClass.isBlank()) {
            LOG.info(">>> SINGLE CLASS MODE: Running only '{}'", singleTestClass);
            suiteDef = new SuiteDefinition(
                    "Single Class Execution",
                    List.of("ALL"),
                    List.of(new SuiteDefinition.TestEntry(singleTestClass, Collections.emptyList())));
        } else {
            LOG.info("Suite: {}", suiteName);
            suiteDef = ConfigLoader.loadSuite(suiteName);
        }

        // Matches the suite requirements against the available operators in the environment
        List<OperatorExecutionPlan> plans = ExecutionPlanner.plan(envConfig.operators(), suiteDef);

        if (plans.isEmpty()) {
            throw new IllegalStateException(
                    "Execution Plan is empty. Check your Environment config and Suite targets.");
        }

        // Generate the Test Graph and Convert the Plans into JUnit 5 Dynamic Nodes
        return plans.stream()
                .map(DynamicOperatorTestFactory::create);
    }

    private void validateContext(String env, String suite, String testClass) {
        if (env == null || env.isBlank()) {
            String error = "MISSING CONFIGURATION: Environment not specified.\n" +
                    "Usage: -Denv=<env_name> (e.g., -Denv=dev)\n" +
                    "Check your Run Configuration or Maven arguments.";
            StepReporter.error(error, null);
            throw new IllegalArgumentException(error);
        }

        if ((suite == null || suite.isBlank()) && (testClass == null || testClass.isBlank())) {
            String error = "MISSING CONFIGURATION: No Suite or Test Class specified.\n" +
                    "Usage A (Suite): -Dsuite=<suite_name> (e.g., -Dsuite=smoke)\n" +
                    "Usage B (Class): -Dtest.class=<full.class.name>";
            StepReporter.error(error, null);
            throw new IllegalArgumentException(error);
        }
    }
}
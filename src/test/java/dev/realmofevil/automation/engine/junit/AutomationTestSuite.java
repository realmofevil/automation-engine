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
        try {
            String envName = System.getProperty("env");
            String suiteName = System.getProperty("suite");
            String testClassFilter = System.getProperty("test.class");
            String testMethodFilter = System.getProperty("test.method");

            validateContext(envName, suiteName, testClassFilter);

            LOG.info("Starting Automation Engine. Environment: {}, Suite: {}", envName, suiteName);
            if (testMethodFilter != null)
                LOG.info("Filter active: Method '{}'", testMethodFilter);

            EnvironmentConfig envConfig = ConfigLoader.loadEnv(envName);
            SuiteDefinition suiteDef;

            Object timeoutObj = null;
            if (envConfig.defaults() != null && envConfig.defaults().contextDefaults() != null) {
                timeoutObj = envConfig.defaults().contextDefaults().get("testTimeoutSeconds");
            }
            String timeout = timeoutObj != null ? String.valueOf(timeoutObj) : "600";

            System.setProperty("junit.jupiter.execution.timeout.default", timeout + " s");
            LOG.info("Global Test Timeout set to: {} seconds", timeout);

            if (testClassFilter != null && !testClassFilter.isBlank()) {
                String target = System.getProperty("operator", "ALL");
                List<String> targets = target.equalsIgnoreCase("ALL") ? List.of("ALL") : List.of(target.split(","));

                LOG.info(">>> SINGLE CLASS MODE: Running only '{}'", testClassFilter);
                suiteDef = new SuiteDefinition("Single Class Execution", targets,
                        List.of(new SuiteDefinition.TestEntry(testClassFilter, Collections.emptyList())));
            } else {
                LOG.info("Suite: {}", suiteName);
                suiteDef = ConfigLoader.loadSuite(suiteName);
                String operatorOverride = System.getProperty("operator");
                if (operatorOverride != null && !operatorOverride.isBlank()) {
                    List<String> targets = operatorOverride.equalsIgnoreCase("ALL")
                            ? List.of("ALL")
                            : List.of(operatorOverride.split(","));

                    LOG.info("CLI Override: Executing suite against operators: {}", targets);
                    suiteDef = new SuiteDefinition(suiteDef.name(), targets, suiteDef.tests());
                }
            }

            List<OperatorExecutionPlan> plans = ExecutionPlanner.plan(envConfig.operators(), suiteDef);

            if (plans.isEmpty()) {
                throw new IllegalStateException(
                        "Execution Plan is empty. Check your Environment config and Suite targets.");
            }

            return plans.stream()
                    .map(plan -> DynamicOperatorTestFactory.create(plan, testMethodFilter));

        } catch (IllegalArgumentException | IllegalStateException e) {
            StepReporter.error("CONFIGURATION ERROR: " + e.getMessage(), null);
            return Stream.empty();
        } catch (RuntimeException e) {
            StepReporter.error("EXECUTION FAILURE: " + e.getMessage(), e);
            throw e;
        }
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
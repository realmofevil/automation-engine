package dev.realmofevil.automation.launcher;

import dev.realmofevil.automation.engine.bootstrap.ConfigLoader;
import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.config.SuiteDefinition;
import dev.realmofevil.automation.engine.execution.ExecutionPlanner;
import dev.realmofevil.automation.engine.execution.OperatorExecutionPlan;
import dev.realmofevil.automation.engine.junit.DynamicOperatorTestFactory;
import dev.realmofevil.automation.engine.reporting.AllureLifecycleManager;
import dev.realmofevil.automation.engine.reporting.ConsoleSummaryListener;
import dev.realmofevil.automation.engine.reporting.StepReporter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Standalone Entry Point.
 * Usage: java -jar automation.jar --env=qa --suite=smoke --operator=ALL
 */
public class AutomationLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(AutomationLauncher.class);

    public static void main(String[] args) {
        LOG.info("Initializing Automation Launcher...");

        Map<String, String> parsedArgs = parseArgs(args);

        parsedArgs.forEach((k, v) -> {
            System.setProperty(k, v);
            LOG.info("Set Property: {} = {}", k, v);
        });

        String envName = System.getProperty("env");
        String suiteName = System.getProperty("suite");

        if (envName == null || suiteName == null) {
            LOG.error("Usage: java -jar automation.jar --env=<env> --suite=<suite> [options]");
            System.exit(1);
        }

        AllureLifecycleManager.cleanResults();
        AllureLifecycleManager.restoreHistory();

        try {
            EnvironmentConfig envConfig = ConfigLoader.loadEnv(envName);
            SuiteDefinition suiteDef = ConfigLoader.loadSuite(suiteName);

            String operatorOverride = System.getProperty("operator");
            if (operatorOverride != null && !operatorOverride.isBlank()) {
                List<String> targets = operatorOverride.equalsIgnoreCase("ALL")
                        ? List.of("ALL")
                        : List.of(operatorOverride.split(","));
                suiteDef = new SuiteDefinition(suiteDef.name(), targets, suiteDef.tests());
            }

            Object timeoutObj = null;
            if (envConfig.defaults() != null && envConfig.defaults().contextDefaults() != null) {
                timeoutObj = envConfig.defaults().contextDefaults().get("testTimeoutSeconds");
            }
            String timeout = timeoutObj != null ? String.valueOf(timeoutObj) : "600";
            System.setProperty("junit.jupiter.execution.timeout.default", timeout + " s");

            AllureLifecycleManager.writeEnvironment(envConfig, suiteName);

            List<OperatorExecutionPlan> plans = ExecutionPlanner.plan(envConfig.operators(), suiteDef);

            String suiteClassName = "dev.realmofevil.automation.engine.junit.AutomationTestSuite";

            try {
                Class.forName(suiteClassName);
            } catch (ClassNotFoundException e) {
                LOG.error(
                        "CRITICAL: Cannot find Test Suite class. Ensure test-classes are in classpath (e.g. shade plugin or IDE config).");
                System.exit(3);
            }

            LauncherDiscoveryRequestBuilder requestBuilder = LauncherDiscoveryRequestBuilder.request();

            requestBuilder.selectors(
                    org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(suiteClassName));

            LauncherDiscoveryRequest request = requestBuilder.build();
            Launcher launcher = LauncherFactory.create();

            org.junit.platform.launcher.listeners.SummaryGeneratingListener internalListener = 
                new org.junit.platform.launcher.listeners.SummaryGeneratingListener();

            launcher.registerTestExecutionListeners(internalListener);

            LOG.info("Launching Test Execution...");
            launcher.execute(request);

            if (internalListener.getSummary().getTotalFailureCount() > 0) {
                System.exit(1);
            }

        } catch (Exception e) {
            StepReporter.error("CRITICAL LAUNCHER FAILURE", e);
            System.exit(2);
        }
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] parts = arg.substring(2).split("=", 2);
                map.put(parts[0], parts[1]);
            }
        }
        return map;
    }
}
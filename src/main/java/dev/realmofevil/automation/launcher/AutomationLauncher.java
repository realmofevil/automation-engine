package dev.realmofevil.automation.launcher;

import dev.realmofevil.automation.engine.reporting.StepReporter;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Entry point for running as a JAR file.
 * Usage: java -jar automation.jar --env=qa --suite=smoke
 */
public class AutomationLauncher {

    public static void main(String[] args) {
        StepReporter.info("Initializing Automation Launcher...");

        Map<String, String> parsedArgs = parseArgs(args);

        parsedArgs.forEach((k, v) -> {
            System.setProperty(k, v);
            StepReporter.info("Override: " + k + " = " + v);
        });

        if (System.getProperty("env") == null) {
            System.err.println("ERROR: Missing required argument: --env");
            System.exit(1);
        }
        if (System.getProperty("suite") == null) {
            System.err.println("ERROR: Missing required argument: --suite");
            System.exit(1);
        }

        launchJUnit();
    }

    private static void launchJUnit() {
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener summaryListener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(summaryListener);

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass("dev.realmofevil.automation.engine.junit.AutomationTestSuite"))
                .build();

        launcher.execute(request);

        TestExecutionSummary summary = summaryListener.getSummary();
        summary.printTo(new PrintWriter(System.out));

        if (summary.getTotalFailureCount() > 0) {
            System.exit(1);
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
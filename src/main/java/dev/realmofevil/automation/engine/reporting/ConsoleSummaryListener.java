package dev.realmofevil.automation.engine.reporting;

import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * Automatically registered via SPI to print summaries in Maven/CI builds.
 * Also used explicitly by AutomationLauncher.
 */
public class ConsoleSummaryListener extends SummaryGeneratingListener {

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        super.testPlanExecutionFinished(testPlan);
        TestExecutionSummary summary = getSummary();
        printExecutionReport(summary);
    }

    private void printExecutionReport(TestExecutionSummary summary) {
        StepReporter.info("=========================================");
        StepReporter.info("       EXECUTION SUMMARY");
        StepReporter.info("=========================================");
        
        StepReporter.info("Total Tests Found: " + summary.getTestsFoundCount());
        StepReporter.info("Total Passed:      " + summary.getTestsSucceededCount());
        StepReporter.info("Total Failed:      " + summary.getTestsFailedCount());
        StepReporter.info("Total Skipped:     " + summary.getTestsSkippedCount());
        StepReporter.info("Time Elapsed:      " + (summary.getTimeFinished() - summary.getTimeStarted()) + "ms");

        if (summary.getTestsFailedCount() > 0) {
            StepReporter.info("-----------------------------------------");
            StepReporter.info("FAILED TESTS:");
            summary.getFailures().forEach(failure -> {
                String testName = failure.getTestIdentifier().getDisplayName();
                String error = failure.getException().getMessage();
                StepReporter.error(" -> " + testName + " | Reason: " + error, failure.getException());
            });
            StepReporter.info("-----------------------------------------");
        }
        StepReporter.info("=========================================");
    }
}
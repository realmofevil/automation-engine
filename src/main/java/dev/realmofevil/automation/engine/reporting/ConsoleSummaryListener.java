package dev.realmofevil.automation.engine.reporting;

import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Automatically registered via SPI to print summaries in Maven/CI builds.
 * Also used explicitly by AutomationLauncher.
 */
public class ConsoleSummaryListener extends SummaryGeneratingListener {
    private static final Logger LOG = LoggerFactory.getLogger("ExecutionSummary");

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        super.testPlanExecutionFinished(testPlan);
        TestExecutionSummary summary = getSummary();
        printExecutionReport(summary);
    }

    private void printExecutionReport(TestExecutionSummary summary) {
        LOG.info("=========================================");
        LOG.info("           EXECUTION SUMMARY");
        LOG.info("=========================================");
        LOG.info("Total Tests Found: " + summary.getTestsFoundCount());
        LOG.info("Total Passed:      " + summary.getTestsSucceededCount());
        LOG.info("Total Failed:      " + summary.getTestsFailedCount());
        LOG.info("Total Skipped:     " + summary.getTestsSkippedCount());
        LOG.info("Time Elapsed:      " + (summary.getTimeFinished() - summary.getTimeStarted()) + "ms");

        if (summary.getTestsFailedCount() > 0) {
            LOG.info("-----------------------------------------");
            LOG.info("FAILED TESTS:");
            summary.getFailures().forEach(failure -> {
                String testName = failure.getTestIdentifier().getDisplayName();
                String error = failure.getException().getMessage();
                LOG.error(" -> " + testName + " | Reason: " + error, failure.getException());
            });
            LOG.info("-----------------------------------------");
        }
        LOG.info("=========================================");
    }
}
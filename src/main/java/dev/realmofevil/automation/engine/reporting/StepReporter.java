package dev.realmofevil.automation.engine.reporting;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade for all reporting activities.
 * Ensures consistent output to both SLF4J (CLI/CI) and Allure (HTML).
 * <p>
 * Enhanced with Thread ID prefixing for observability in parallel execution.
 * </p>
 */
public final class StepReporter {
    private static final Logger LOG = LoggerFactory.getLogger("TestStep");

    private StepReporter() {}

    /**
     * Prepends the current Thread ID to the log message.
     * Essential for distinguishing interleaved logs in parallel execution.
     */
    private static String prefix(String message) {
        return String.format("[TID:%d] %s", Thread.currentThread().threadId(), message);
    }

    public static void info(String message) {
        LOG.info(prefix(message));
        Allure.step(message);
    }

    /**
     * Records a successful verification or major milestone.
     */
    public static void pass(String message) {
        LOG.info(prefix("✅ PASS: " + message));
        Allure.step("✅ " + message);
    }

    public static void warn(String message) {
        LOG.warn(prefix(message));
        Allure.step("WARN: " + message);
    }

    public static void error(String message, Throwable t) {
        LOG.error(prefix(message), t);
        Allure.step("ERROR: " + message);
    }

    public static void attachJson(String title, String json) {
        String preview = (json != null && json.length() > 100) ? json.substring(0, 100) + "..." : json;
        LOG.info(prefix("[Attachment] {}: {}"), title, preview);
        if (json != null) {
            Allure.addAttachment(title, "application/json", json);
        }
    }

    public static void attachText(String title, String content) {
        String preview = (content != null && content.length() > 500) ? content.substring(0, 500) + "..." : content;
        LOG.debug(prefix("[Attachment] " + title + ":\n" + preview));
        Allure.addAttachment(title, "text/plain", content);
    }
}
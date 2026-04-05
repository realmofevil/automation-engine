package dev.realmofevil.automation.engine.reporting;

import dev.realmofevil.automation.engine.context.ContextHolder;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

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
        String operatorId = "INIT";
        try {
            if (ContextHolder.isSet()) {
                operatorId = ContextHolder.get().config().id();
            }
        } catch (Exception ignored) {}

        return String.format("[TID:%d|OP:%s] %s", Thread.currentThread().threadId(), operatorId, message);
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
        LOG.error(prefix(message));
        Allure.step("ERROR: " + message);

        if (t != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);

            Allure.addAttachment("Exception Stacktrace", "text/plain", sw.toString());
        }
    }

    public static void attachJson(String title, String json) {
        if (json == null) return;
        String safeContent = SmartRedactor.mask(json);

        String singleLineContent = safeContent.replace("\r", "").replace("\n", " ").replace("\t", " ");
        String preview = (singleLineContent.length() > 200) ? singleLineContent.substring(0, 197) + "..." : singleLineContent;

        LOG.info(prefix("[Attachment] " + title + ": " + preview));
        Allure.addAttachment(title, "application/json", safeContent);
    }

    public static void attachText(String title, String content) {
        if (content == null) return;
        String safeContent = SmartRedactor.mask(content);

        String preview = (safeContent.length() > 500) ? safeContent.substring(0, 497) + "..." : safeContent;

        LOG.info(prefix("[Attachment] " + title + ":\n" + preview));
        Allure.addAttachment(title, "text/plain", safeContent);
    }
}
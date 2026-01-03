package dev.realmofevil.automation.engine.reporting;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade for all reporting activities.
 * Ensures consistent output to both SLF4J (CLI/CI) and Allure (HTML).
 */
public final class StepReporter {
    
    private static final Logger LOG = LoggerFactory.getLogger("TestStep");

    private StepReporter() {}

    public static void info(String message) {
        LOG.info(message);
        Allure.step(message);
    }

    public static void warn(String message) {
        LOG.warn(message);
        Allure.step("WARN: " + message);
    }

    public static void error(String message, Throwable t) {
        LOG.error(message, t);
        Allure.step("ERROR: " + message);
    }

    public static void attachJson(String title, String json) {
        LOG.debug("[Attachment] {}: {}", title, (json != null && json.length() > 100) ? json.substring(0, 100) + "..." : json);
        if (json != null) {
            Allure.addAttachment(title, "application/json", json);
        }
    }
    
    public static void attachText(String title, String content) {
        LOG.debug("[Attachment] {}", title);
        Allure.addAttachment(title, "text/plain", content);
    }
}
package dev.realmofevil.automation.engine.reporting;

import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TestReporter {

    private static final Logger LOG =
            LoggerFactory.getLogger(TestReporter.class);

    private TestReporter() {}

    public static void info(String message) {
        LOG.info(message);
        Allure.step(message);
    }

    public static void warn(String message) {
        LOG.warn(message);
        Allure.step("WARN: " + message);
    }

    public static void attach(String name, String content) {
        Allure.addAttachment(name, content);
    }
}
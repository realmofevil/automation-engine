package dev.realmofevil.automation.engine.reporting;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import io.qameta.allure.Allure;

public final class OperatorAllure {

    private OperatorAllure() {}

    public static void applyContext(ExecutionContext ctx) {
        Allure.label("operator", ctx.operator().name());
        Allure.label("environment", ctx.environment().name());
        Allure.parameter("domain", ctx.operator().domain());
    }

    public static void info(String message) {
        Allure.step(message);
        System.out.println("[INFO] " + message);
    }
}
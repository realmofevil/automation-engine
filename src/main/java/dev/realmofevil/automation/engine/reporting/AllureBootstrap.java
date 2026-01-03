package dev.realmofevil.automation.engine.reporting;

import io.qameta.allure.Allure;

public final class AllureBootstrap {

    public static void apply(ReportingContext ctx) {

        Allure.label("environment", ctx.environment());
        Allure.label("operator", ctx.operator());
        Allure.label("suite", ctx.suite());
        Allure.label("executionId", ctx.executionId().toString());
    }
}
package dev.realmofevil.automation.engine.reporting;

import io.qameta.allure.Allure;
import dev.realmofevil.automation.engine.context.ExecutionContext;

public final class AllureBootstrap {

    private AllureBootstrap() {} // remove?

    public static void init() {
        // Allure.getLifecycle();
        var ctx = ExecutionContext.get();
        Allure.label("environment", ctx.environment().name());
        Allure.label("operator",
                ctx.operator() == null ? "generic" : ctx.operator().name());
    }
}

package dev.realmofevil.automation.engine.reporting;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.context.ExecutionContextHolder;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.*;

public final class AllureExecutionListener
        implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        ExecutionContext ctx = ExecutionContextHolder.get();

        Allure.label("operator", ctx.operator().id());
        Allure.label("environment", ctx.operator().environment());
        Allure.label("domain.desktop",
                ctx.operator().desktopDomain().toString());
    }
}
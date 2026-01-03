package dev.realmofevil.automation.engine.junit;

import dev.realmofevil.automation.engine.auth.AccountCredentials;
import dev.realmofevil.automation.engine.auth.annotations.UseAccount;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.context.ExecutionContextHolder;
import org.junit.jupiter.api.extension.*;

public final class AccountPinningInterceptor
        implements BeforeTestExecutionCallback,
                   AfterTestExecutionCallback {

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        ExecutionContext ctx = ExecutionContextHolder.get();

        context.getElement()
                .flatMap(el -> el.getAnnotation(UseAccount.class) != null
                        ? java.util.Optional.of(
                                el.getAnnotation(UseAccount.class))
                        : java.util.Optional.empty()
                )
                .ifPresent(annotation -> {
                    AccountCredentials acc =
                            ctx.accounts()
                                    .byId(annotation.id());
                    ctx.accounts().pin(acc);
                });
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        ExecutionContext ctx = ExecutionContextHolder.get();
        ctx.accounts().clearPin();
    }
}

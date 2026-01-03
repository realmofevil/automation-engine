package dev.realmofevil.automation.engine.junit;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.context.ExecutionContextHolder;
import dev.realmofevil.automation.engine.db.annotations.CommitTransaction;
import org.junit.jupiter.api.extension.*;

public final class TransactionInterceptor
        implements BeforeTestExecutionCallback,
                   AfterTestExecutionCallback {

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        ExecutionContext ctx = ExecutionContextHolder.get();
        ctx.transactions().begin();
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        ExecutionContext ctx = ExecutionContextHolder.get();

        context.getElement()
                .filter(el -> el.isAnnotationPresent(CommitTransaction.class))
                .ifPresent(el ->
                        ctx.transactions()
                                .current()
                                .requestCommit()
                );

        ctx.transactions().end();
    }
}
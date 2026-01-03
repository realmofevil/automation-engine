package dev.realmofevil.automation.engine.junit;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.extension.*;

public final class FlakyRetryExtension
        implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(
            ExtensionContext context,
            Throwable throwable) throws Throwable {

        Flaky flaky = context
                .getElement()
                .flatMap(el -> AnnotationSupport.findAnnotation(el, Flaky.class))
                .orElse(null);

        if (flaky == null) {
            throw throwable;
        }

        int attempt = context
                .getStore(ExtensionContext.Namespace.GLOBAL)
                .getOrComputeIfAbsent(
                        context.getUniqueId(),
                        k -> 0,
                        Integer.class);

        int retries = executionContext
                .suite()
                .flakiness()
                .retries();

        if (attempt < flaky.retries()) {
            context.getStore(ExtensionContext.Namespace.GLOBAL)
                    .put(context.getUniqueId(), attempt + 1);

            Allure.label("flaky", "true");
            Allure.step("Retrying flaky test, attempt " + (attempt + 1));

            throw throwable;
        }

        /**
         * if (flakyRate > policy.quarantine().maxFlakyRatePercent()) {
         * throw new AssertionError(
         * "Flaky rate exceeded threshold: " + flakyRate + "%"
         * );
         * }
         * 
         **/

        throw throwable;
    }
}

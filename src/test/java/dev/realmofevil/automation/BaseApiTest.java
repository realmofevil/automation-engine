
package dev.realmofevil.automation;

import dev.realmofevil.automation.engine.context.ExecutionContextHolder;
import dev.realmofevil.automation.engine.reporting.OperatorAllure;
import org.junit.jupiter.api.BeforeEach;

public abstract class BaseApiTest {

    @BeforeEach
    void before() {
        OperatorAllure.applyContext(ExecutionContextHolder.get());
    }

    protected void info(String msg) {
        OperatorAllure.info(msg);
    }
}

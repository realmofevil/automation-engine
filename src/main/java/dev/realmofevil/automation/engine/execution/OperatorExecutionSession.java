package dev.realmofevil.automation.engine.execution;

import dev.realmofevil.automation.engine.context.ExecutionContextHolder;

public final class OperatorExecutionSession implements AutoCloseable {

    private final OperatorRuntime runtime;

    public OperatorExecutionSession(OperatorRuntime runtime) {
        this.runtime = runtime;
    }

    public OperatorRuntime runtime() {
        return runtime;
    }

    public void beforeTest() {
        ExecutionContextHolder.set(runtime.context());
    }

    public void afterTest() {
        ExecutionContextHolder.clear();
    }

    @Override
    public void close() {
        runtime.close();
    }
}
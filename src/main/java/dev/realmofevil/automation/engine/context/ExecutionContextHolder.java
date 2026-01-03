package dev.realmofevil.automation.engine.context;

import java.util.Objects;

public final class ExecutionContextHolder {

    private static final ThreadLocal<ExecutionContext> CURRENT = new ThreadLocal<>();

    private ExecutionContextHolder() {}

    public static void set(ExecutionContext context) {
        CURRENT.set(Objects.requireNonNull(context));
    }

    public static ExecutionContext get() {
        ExecutionContext ctx = CURRENT.get();
        if (ctx == null) {
            throw new IllegalStateException("ExecutionContext not initialized");
        }
        return ctx;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
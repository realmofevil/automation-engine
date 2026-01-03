package dev.realmofevil.automation.engine.context;

public final class ExecutionContextHolder {

    private static final ThreadLocal<ExecutionContext> CTX = new ThreadLocal<>();

    private ExecutionContextHolder() {}

    public static void set(ExecutionContext ctx) {
        CTX.set(ctx);
    }

    public static ExecutionContext get() {
        ExecutionContext ctx = CTX.get();
        if (ctx == null) {
            throw new IllegalStateException("ExecutionContext not set for thread");
        }
        return ctx;
    }

    public static void clear() {
        CTX.remove();
    }
}
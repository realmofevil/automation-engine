package dev.realmofevil.automation.engine.context;

public final class ContextHolder {
    private static final ThreadLocal<ExecutionContext> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(ExecutionContext ctx) {
        THREAD_LOCAL.set(ctx);
    }

    public static ExecutionContext get() {
        ExecutionContext ctx = THREAD_LOCAL.get();
        if (ctx == null) {
            throw new IllegalStateException("No ExecutionContext active on this thread. Test setup failure?");
        }
        return ctx;
    }

    public static boolean isSet() {
        return THREAD_LOCAL.get() != null;
    }

    public static void clear() {
        THREAD_LOCAL.remove();
    }
}
package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.config.AuthConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.auth.annotations.UseDedicatedAccount;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class AuthManager {

    private static final Map<String, AccountPool> POOLS = new ConcurrentHashMap<>();

    private static final ThreadLocal<Boolean> DEDICATED = ThreadLocal.withInitial(() -> false);

    private AuthManager() {
    }

    public static void init() {
        ExecutionContext.environment()
                .operators()
                .forEach((operator, endpoint) -> {
                    AuthConfig auth = endpoint.auth();
                    if (auth != null && auth.accounts() != null) {
                        POOLS.put(operator, new AccountPool(auth.accounts()));
                    }
                });
    }

    /**
     * Sets dedicated account mode programmatically. Framework-agnostic.
     * @param dedicated true to use dedicated account, false for shared
     */
    public static void beforeTest(boolean dedicated) {
        DEDICATED.set(dedicated);
    }

    /**
     * Auto-detects dedicated account mode from @UseDedicatedAccount annotation.
     * Convenience overload for JUnit 5 tests.
     * @param context the JUnit extension context
     */
    public static void beforeTest(ExtensionContext context) {
        boolean useDedicated = context.getTestMethod()
                .map(m -> m.isAnnotationPresent(UseDedicatedAccount.class))
                .orElse(false);
        beforeTest(useDedicated);
    }

    public static void acquire() {
        String operator = ExecutionContext.operator();
        AccountPool pool = POOLS.get(operator);
        if (pool == null)
            return;

        AuthSession session = pool.acquire(DEDICATED.get());
        AuthContext.set(session);
    }

    public static void release() {
        String operator = ExecutionContext.operator();
        AccountPool pool = POOLS.get(operator);

        if (pool != null) {
            pool.release(AuthContext.get());
        }

        AuthContext.clear();
        DEDICATED.remove();
    }
}

/**
package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.config.OperatorEndpoint;

public final class AuthManager {

    private AuthManager() {}

    public static void acquire() {
        OperatorEndpoint endpoint = ExecutionContext.endpoint();
        AuthConfig auth = endpoint.auth();

        if (auth == null || auth.type() == AuthType.NONE) {
            return;
        }

        switch (auth.type()) {
            case BASIC -> BasicAuthProvider.login(auth);
            case TOKEN -> TokenAuthProvider.login(auth);
            case SESSION -> SessionAuthProvider.login(auth);
        }
    }

    public static void release() {
        OperatorEndpoint endpoint = ExecutionContext.endpoint();
        AuthConfig auth = endpoint.auth();

        if (auth == null || auth.type() == AuthType.NONE) {
            return;
        }

        switch (auth.type()) {
            case SESSION -> SessionAuthProvider.logout();
            default -> {
                // nothing to release
            }
        }
    }
}
**/
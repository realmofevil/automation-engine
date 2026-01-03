package dev.realmofevil.automation.engine.auth;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that automatically manages auth sessions.
 * Tests can use @UseDedicatedAccount to request dedicated accounts.
 */
public class AccountSessionExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        AuthManager.beforeTest(context);
        AuthManager.acquire();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        AuthManager.release();
    }
}
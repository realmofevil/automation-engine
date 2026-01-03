package dev.realmofevil.automation.engine.junit;

import dev.realmofevil.automation.engine.auth.AuthManager;
import org.junit.jupiter.api.extension.*;

public class AuthExtension
        implements BeforeEachCallback, AfterEachCallback {

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

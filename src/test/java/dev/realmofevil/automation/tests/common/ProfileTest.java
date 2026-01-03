package dev.realmofevil.automation.tests.common;

import dev.realmofevil.automation.engine.context.ContextHolder;
import org.junit.jupiter.api.Test;

public class ProfileTest {

    @Test
    void userProfileLoads() {
        var ctx = ContextHolder.get();

        // Explicitly ensure we are logged in as the 'vip' user defined in YAML
        // This triggers LoginClient > reads YAML > POST /login > Extracts Token > Sets Session
        ctx.authManager().ensureAuthenticated("vip");

        // ApiClient automatically attaches the token from AuthSession
        var response = ctx.api().get("user.profile");
        
        response.assertOk();
    }
}
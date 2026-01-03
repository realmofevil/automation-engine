package dev.realmofevil.automation.tests.common;

import dev.realmofevil.automation.engine.context.ContextHolder;
import org.junit.jupiter.api.Test;

public class ProfileTest {

    @Test
    void userProfileLoads() {
        var ctx = ContextHolder.get();

        ctx.authManager().ensureAuthenticated("vip");

        var response = ctx.api().get("user.profile");
        
        response.assertOk();
    }
}
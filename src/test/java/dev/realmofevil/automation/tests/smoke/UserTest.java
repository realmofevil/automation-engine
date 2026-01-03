package dev.realmofevil.automation.tests.smoke;

import dev.realmofevil.automation.engine.context.ContextHolder;
import dev.realmofevil.automation.engine.http.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UserTest {

    @Test
    void verifyUserProfile() {
        var ctx = ContextHolder.get();
        ctx.authManager().ensureAuthenticated("default");

        Response response = ctx.api()
                .route("user.profile")
                .query("includeDetails", "true")
                .header("X-Client-Version", "1.0")
                .get();

        response.assertOk();

        UserProfile profile = response.as(UserProfile.class);
        Assertions.assertNotNull(profile.email());

        var dbUsers = ctx.db().query(
            "SELECT email FROM users WHERE username = ?", 
            rs -> rs.getString("email"),
            profile.username()
        );
        
        Assertions.assertFalse(dbUsers.isEmpty());
        Assertions.assertEquals(profile.email(), dbUsers.get(0));
    }
    
    record UserProfile(String username, String email) {}
}
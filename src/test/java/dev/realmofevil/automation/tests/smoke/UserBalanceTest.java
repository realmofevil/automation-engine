package dev.realmofevil.automation.tests.smoke;

import dev.realmofevil.automation.engine.context.ContextHolder;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserBalanceTest {

    @Test
    void verifyBalanceUpdatesAfterLock() throws Exception {
        ExecutionContext ctx = ContextHolder.get();

        ctx.auth().loginBasic("testuser", "password123");

        var payload = new BalanceLockRequest(10.0, "EUR");
        Response response = ctx.api().post("payment.lockBalance", payload);
        
        response.assertOk();

        try (Connection conn = ctx.db().getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT balance_locked FROM users WHERE username = ?");
            stmt.setString(1, "testuser");
            ResultSet rs = stmt.executeQuery();
            
            Assertions.assertTrue(rs.next(), "User not found in DB");
            Assertions.assertEquals(10.0, rs.getDouble("balance_locked"));
        }
    }

    record BalanceLockRequest(double amount, String currency) {}
}
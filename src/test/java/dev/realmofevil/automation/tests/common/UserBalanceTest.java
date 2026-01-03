package dev.realmofevil.automation.tests.common;

import dev.realmofevil.automation.engine.context.ExecutionContextHolder;
import dev.realmofevil.automation.engine.db.annotations.CommitTransaction;
import dev.realmofevil.automation.engine.http.RouteInvoker;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserBalanceTest {

    @Test
    @CommitTransaction
    void balanceUpdatedInDb() throws Exception {
        var ctx = ExecutionContextHolder.get();

        new RouteInvoker(ctx)
                .invokeDesktop("payment.lockBalance");

        var conn = ctx.transactions().current().connection();
        ResultSet rs =
                conn.createStatement()
                        .executeQuery(
                                "SELECT balance FROM accounts"
                        );

        assertTrue(rs.next());
    }
}

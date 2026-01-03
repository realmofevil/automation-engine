package dev.realmofevil.automation.engine.db;

import dev.realmofevil.automation.engine.reporting.StepReporter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A wrapper around JDBC to provide logging and simplified mapping.
 */
public class DbClient {
    private final TransactionManager txManager;

    public DbClient(TransactionManager txManager) {
        this.txManager = txManager;
    }

    public <T> List<T> query(String sql, Function<ResultSet, T> mapper, Object... params) {
        StepReporter.info("DB Query: " + sql);
        
        Connection conn = txManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.apply(rs));
                }
                StepReporter.attachText("DB Result Count", String.valueOf(results.size()));
                return results;
            }
        } catch (SQLException e) {
            StepReporter.error("DB Query Failed", e);
            throw new RuntimeException(e);
        }
    }
}
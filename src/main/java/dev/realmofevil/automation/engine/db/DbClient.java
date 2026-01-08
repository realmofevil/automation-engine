package dev.realmofevil.automation.engine.db;

import dev.realmofevil.automation.engine.reporting.StepReporter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A wrapper around JDBC to provide logging and simplified mapping.
 */
public class DbClient {
    private final TransactionManager txManager;

    public DbClient(TransactionManager txManager) {
        this.txManager = txManager;
    }

    public <T> List<T> query(String sql, SqlMapper<T> mapper, Object... params) {
        StepReporter.info("DB Query: " + sql);

        Connection conn = txManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
                StepReporter.attachText("DB Result Count", String.valueOf(results.size()));
                return results;
            }
        } catch (SQLException e) {
            StepReporter.error("DB Query Failed: " + e.getMessage(), e);
            throw new RuntimeException("Database execution failed", e);
        }
    }

    public <T> Optional<T> querySingle(String sql, SqlMapper<T> mapper, Object... params) {
        List<T> results = query(sql, mapper, params);
        if (results.size() > 1) {
            StepReporter.warn("DB Query returned " + results.size() + " rows, expected 1. Using first.");
        }
        return results.stream().findFirst();
    }

    public <T> Optional<T> queryScalar(String sql, Class<T> type, Object... params) {
        return querySingle(sql, rs -> rs.getObject(1, type), params);
    }

    public boolean exists(String sql, Object... params) {
        // return query(sql, rs -> true, params).size() > 0;
        List<Integer> result = query(sql, rs -> 1, params);
        return !result.isEmpty();
    }

    public int execute(String sql, Object... params) {
        StepReporter.info("DB Execute: " + sql);

        Connection conn = txManager.getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            int updateCount = stmt.executeUpdate();
            StepReporter.attachText("DB Update Count", String.valueOf(updateCount));
            return updateCount;
        } catch (SQLException e) {
            StepReporter.error("DB Execute Failed: " + e.getMessage(), e);
            throw new RuntimeException("Database execution failed", e);
        }
    }

}
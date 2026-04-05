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

        try (Connection conn = txManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            throw handleSqlException(e, sql);
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

        try (Connection conn = txManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            int updateCount = stmt.executeUpdate();
            StepReporter.attachText("DB Update Count", String.valueOf(updateCount));
            return updateCount;
        } catch (SQLException e) {
            throw handleSqlException(e, sql);
        }
    }

    /**
     * Translates raw SQLExceptions using standard SQLState codes.
     */
    private RuntimeException handleSqlException(SQLException e, String sql) {
        String state = e.getSQLState();
        if (state == null)
            state = "UNKNOWN";

        String message = switch (state.substring(0, 2)) {
            case "08" -> "DATABASE CONNECTION FAILED: Unable to reach the database server. Check VPN, Host URL, or Firewall rules.";
            case "28" -> "DATABASE AUTHENTICATION FAILED: Invalid username or password. Check YAML DB credentials.";
            case "3D" -> "DATABASE CATALOG ERROR: The specified database name does not exist.";
            case "42" -> "SQL SYNTAX OR ACCESS ERROR: Query is malformed, or the test user lacks permissions for this table/view.";
            default -> "DATABASE EXECUTION ERROR: " + e.getMessage();
        };

        String fullError = String.format("%s | SQL State: %s | Query: %s", message, state, sql);
        StepReporter.error("DB FAILURE | " + fullError, null);

        return new RuntimeException(fullError, e);
    }
}
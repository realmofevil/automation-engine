package dev.realmofevil.automation.engine.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionManager {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionManager.class);
    private final DataSource dataSource;
    private final ThreadLocal<Connection> currentConnection = new ThreadLocal<>();

    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void begin() {
        if (currentConnection.get() != null) {
            throw new IllegalStateException("Transaction already active on this thread");
        }
        try {
            Connection conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            currentConnection.set(conn);
            LOG.debug("Transaction started");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to begin transaction", e);
        }
    }

    public Connection getConnection() {
        Connection conn = currentConnection.get();
        if (conn == null) {
            throw new IllegalStateException("No active transaction. Did you forget @Test transaction setup?");
        }
        return conn;
    }

    public void end(boolean commit) {
        Connection conn = currentConnection.get();
        if (conn == null) return;

        try {
            if (commit) {
                conn.commit();
                LOG.debug("Transaction committed");
            } else {
                conn.rollback();
                LOG.debug("Transaction rolled back");
            }
        } catch (SQLException e) {
            LOG.error("Failed to end transaction", e);
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                LOG.warn("Failed to close connection", e);
            }
            currentConnection.remove();
        }
    }
}
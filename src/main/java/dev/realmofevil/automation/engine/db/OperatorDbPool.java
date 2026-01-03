package dev.realmofevil.automation.engine.db;

import dev.realmofevil.automation.engine.security.CredentialDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public final class OperatorDbPool implements AutoCloseable {

    private final List<PooledConnection> pool;

    public OperatorDbPool(DatabaseConfig config) {
        this.pool = new ArrayList<>();

        try {
            for (int i = 0; i < config.poolSize(); i++) {
                Connection conn =
                        DriverManager.getConnection(
                                config.jdbcUrl(),
                                CredentialDecoder.decode(config.usernameB64()),
                                CredentialDecoder.decode(config.passwordB64())
                        );
                conn.setAutoCommit(false);
                pool.add(new PooledConnection(conn));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize DB pool", e);
        }
    }

    public Connection acquire() {
        synchronized (pool) {
            for (PooledConnection pc : pool) {
                if (pc.tryAcquire()) {
                    return pc.connection();
                }
            }
        }
        throw new IllegalStateException("No DB connections available");
    }

    public void release(Connection connection) {
        synchronized (pool) {
            pool.stream()
                    .filter(pc -> pc.connection() == connection)
                    .findFirst()
                    .ifPresent(PooledConnection::release);
        }
    }

    @Override
    public void close() {
        pool.forEach(pc -> {
            try {
                pc.connection().close();
            } catch (Exception ignored) {}
        });
    }
}
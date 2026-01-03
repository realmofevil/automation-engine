package dev.realmofevil.automation.engine.db;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.config.DatabaseConfig;

import java.sql.Connection;

public final class DbContext {

    private static final ThreadLocal<Connection> connection =
            new ThreadLocal<>();

    private DbContext() {}

    public static Connection get() {
        Connection conn = connection.get();
        if (conn == null) {
            DatabaseConfig dbConfig =
                    ExecutionContext.endpoint().database();

            if (dbConfig == null) {
                throw new IllegalStateException(
                        "Database not configured for operator: "
                                + ExecutionContext.operator()
                );
            }

            conn = DbConnectionFactory.create(dbConfig);
            connection.set(conn);
        }
        return conn;
    }

    public static void close() {
        Connection conn = connection.get();
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception ignored) {
            } finally {
                connection.remove();
            }
        }
    }
}

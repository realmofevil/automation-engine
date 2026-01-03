package dev.realmofevil.automation.engine.db;

import dev.realmofevil.automation.engine.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;

public final class DbConnectionFactory {

    private DbConnectionFactory() {}

    public static Connection create(DatabaseConfig config) {
        try {
            Class.forName(config.driver().driverClass());
            return DriverManager.getConnection(
                    config.url(),
                    config.username(),
                    config.password());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DB connection", e);
        }
    }
}

// // Alternative implementation using HikariCP for connection pooling
// import com.zaxxer.hikari.HikariConfig;
// import com.zaxxer.hikari.HikariDataSource;

// import javax.sql.DataSource;

// public final class DbConnectionFactory {

//     private DbConnectionFactory() {}

//     public static DataSource create(
//             String jdbcUrl,
//             String user,
//             String pass
//     ) {
//         HikariConfig cfg = new HikariConfig();
//         cfg.setJdbcUrl(jdbcUrl);
//         cfg.setUsername(user);
//         cfg.setPassword(pass);
//         cfg.setMaximumPoolSize(5);
//         cfg.setAutoCommit(false);
//         return new HikariDataSource(cfg);
//     }
// }

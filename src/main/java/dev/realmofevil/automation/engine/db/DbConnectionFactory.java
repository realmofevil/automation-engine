package dev.realmofevil.automation.engine.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class DbConnectionFactory {

    private DbConnectionFactory() {}

    public static DataSource create(String jdbcUrl, String user, String pass) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbcUrl);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(5);
        cfg.setAutoCommit(false);
        return new HikariDataSource(cfg);
    }
}
package dev.realmofevil.automation.engine.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.realmofevil.automation.engine.config.OperatorConfig;

import javax.sql.DataSource;

public final class DataSourceFactory {

    private DataSourceFactory() {}

    public static DataSource create(OperatorConfig.DbConfig config) {
        if (config == null) return null;

        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.jdbcUrl());

        hikari.setUsername(config.username().plainText());
        hikari.setPassword(config.password().plainText());

        hikari.setMaximumPoolSize(5); // TODO: Could be configurable
        hikari.setConnectionTimeout(10000);
        hikari.setAutoCommit(false); // Managed by TransactionManager

        if (config.type() != null && config.type().equalsIgnoreCase("oracle")) {
            hikari.setDriverClassName("oracle.jdbc.OracleDriver");
        } else if (config.type() != null && config.type().equalsIgnoreCase("postgresql")) {
            hikari.setDriverClassName("org.postgresql.Driver");
        }

        return new HikariDataSource(hikari);
    }
}
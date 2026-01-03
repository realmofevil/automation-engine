package dev.realmofevil.automation.engine.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.realmofevil.automation.engine.config.OperatorConfig;
import javax.sql.DataSource;

public final class DataSourceFactory {

    private DataSourceFactory() {}

    public static DataSource create(OperatorConfig.DbConfig config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.jdbcUrl());
        hikari.setUsername(config.username().plainText());
        hikari.setPassword(config.password().plainText());
        hikari.setMaximumPoolSize(5); 
        hikari.setConnectionTimeout(5000);
        hikari.setAutoCommit(false); // Enable transaction management
        
        return new HikariDataSource(hikari);
    }
}
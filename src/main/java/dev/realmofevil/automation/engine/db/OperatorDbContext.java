package dev.realmofevil.automation.engine.db;

import javax.sql.DataSource;

public record OperatorDbContext(DbVendor vendor, DataSource dataSource) {}
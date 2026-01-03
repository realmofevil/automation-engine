package dev.realmofevil.automation.engine.db;

public record DatabaseConfig(
        DatabaseType type,
        String jdbcUrl,
        String usernameB64,
        String passwordB64,
        int poolSize
) {}
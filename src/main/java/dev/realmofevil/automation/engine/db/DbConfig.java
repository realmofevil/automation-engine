package dev.realmofevil.automation.engine.db;

public record DbConfig(
        String type,
        String host,
        int port,
        String database,
        DbCredentials credentials
) {}
package dev.realmofevil.automation.engine.config;

public record DatabaseConfig(
        DbDriver driver,
        String url,
        String username,
        String password
) {}

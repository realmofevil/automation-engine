package dev.realmofevil.automation.engine.config;

public record LoginConfig(
        String endpoint,
        String method,
        String tokenField,
        String sessionCookie
) {}

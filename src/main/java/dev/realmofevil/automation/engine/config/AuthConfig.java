package dev.realmofevil.automation.engine.config;

import java.util.List;

public record AuthConfig(
        List<AuthMechanism> mechanisms,
        LoginConfig login,
        List<AuthAccount> accounts
) {}

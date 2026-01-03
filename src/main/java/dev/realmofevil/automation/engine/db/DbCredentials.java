package dev.realmofevil.automation.engine.db;

import java.util.Objects;

public record DbCredentials(
        String username,
        String password) {
    public DbCredentials {
        Objects.requireNonNull(username, "username cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        if (username.isBlank()) {
            throw new IllegalArgumentException("username cannot be blank");
        }
    }
}
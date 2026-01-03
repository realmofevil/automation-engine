package dev.realmofevil.automation.engine.operator;

import java.util.Objects;

public record ApiAccount(String username, String password) {
    public ApiAccount {
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
    }
}
package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.security.CredentialDecoder;

public record AccountCredentials(
        String id,
        String usernameB64,
        String passwordB64) {
    public String username() {
        return CredentialDecoder.decode(usernameB64);
    }

    public String password() {
        return CredentialDecoder.decode(passwordB64);
    }
}
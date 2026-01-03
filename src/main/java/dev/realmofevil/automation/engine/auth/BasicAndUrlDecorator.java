package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.security.CredentialDecoder;

public class BasicAndUrlDecorator {
    String user = CredentialDecoder.decode(creds.usernameB64());
    String pass = CredentialDecoder.decode(creds.passwordB64());
}
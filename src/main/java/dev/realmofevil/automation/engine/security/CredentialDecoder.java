package dev.realmofevil.automation.engine.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class CredentialDecoder {

    private CredentialDecoder() {}

    public static String decode(String base64) {
        return new String(
                Base64.getDecoder().decode(base64),
                StandardCharsets.UTF_8);
    }
}
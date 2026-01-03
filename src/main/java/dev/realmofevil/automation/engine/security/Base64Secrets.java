package dev.realmofevil.automation.engine.security;

import java.util.Base64;

public final class Base64Secrets {

    private Base64Secrets() {}

    public static String decode(String encoded) {
        return new String(
                Base64.getDecoder().decode(encoded)
        );
    }
}

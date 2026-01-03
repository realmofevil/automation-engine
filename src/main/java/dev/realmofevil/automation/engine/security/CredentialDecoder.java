package dev.realmofevil.automation.engine.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Central utility for handling credential decoding.
 * Enforces Base64 encoding for secrets but allows graceful fallbacks for local debugging.
 */
public final class CredentialDecoder {
    
    private static final Logger LOG = LoggerFactory.getLogger(CredentialDecoder.class);

    private CredentialDecoder() {}

    /**
     * Decodes a Base64 string.
     * @param encoded The Base64 encoded string.
     * @return The plain text string.
     */
    public static String decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        
        if (encoded.isBlank()) {
            return "";
        }

        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            // useful during local development if used "password" instead of "cGFzc3dvcmQ="
            LOG.warn("Credential '{}' is not valid Base64. Using raw value. Ensure secrets are encoded in production config.", 
                    obfuscate(encoded));
            return encoded;
        }
    }
    
    private static String obfuscate(String input) {
        if (input == null || input.length() < 3) return "***";
        return input.substring(0, 2) + "***";
    }
}
package dev.realmofevil.automation.engine.config;

import dev.realmofevil.automation.engine.security.CredentialDecoder;
import java.util.Objects;

/**
 * Value Object for sensitive configuration data.
 * <p>
 * Responsibilities:
 * 1. Holds the encrypted/encoded value in memory.
 * 2. Prevents accidental logging via toString() masking.
 * 3. Provides explicit access to plain text via lazy decoding.
 * </p>
 */
public final class Secret {

    private final String rawValue;

    /**
     * Constructor used by SnakeYAML.
     * 
     * @param value The raw value from the YAML file (usually Base64).
     */
    public Secret(String value) {
        this.rawValue = value;
    }

    /**
     * Returns the decoded plain text password/token.
     */
    public String plainText() {
        return CredentialDecoder.decode(rawValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Secret secret = (Secret) o;
        return Objects.equals(rawValue, secret.rawValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawValue);
    }

    @Override
    public String toString() {
        return "*****"; // Security: Never print the value
    }
}
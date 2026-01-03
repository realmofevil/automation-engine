package dev.realmofevil.automation.engine.config;

/**
 * Thrown when framework configuration is invalid or incomplete.
 * Fail-fast exception to prevent test execution with broken setup.
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}

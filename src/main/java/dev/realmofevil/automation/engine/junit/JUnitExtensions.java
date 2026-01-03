package dev.realmofevil.automation.engine.junit;

import org.junit.jupiter.api.extension.Extension;

public final class JUnitExtensions {

    public static Extension[] all() {
        return new Extension[] {
                new FlakyRetryExtension()
        };
    }
}
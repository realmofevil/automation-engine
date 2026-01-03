package dev.realmofevil.automation.engine.util;

import java.io.InputStream;

public final class ResourceLoader {

    private ResourceLoader() {}

    public static InputStream load(String path) {
        InputStream in =
                ResourceLoader.class
                        .getClassLoader()
                        .getResourceAsStream(path);

        if (in == null) {
            throw new IllegalArgumentException(
                    "Resource not found: " + path);
        }
        return in;
    }
}

package dev.realmofevil.automation.engine.operator;

import java.net.URI;
import java.util.Objects;

public final class OperatorDomains {

    private final URI desktop;
    private final URI mobile;

    public OperatorDomains(URI desktop, URI mobile) {
        this.desktop = Objects.requireNonNull(desktop);
        this.mobile = Objects.requireNonNull(mobile);
    }

    public URI desktop() {
        return desktop;
    }

    public URI mobile() {
        return mobile;
    }
}
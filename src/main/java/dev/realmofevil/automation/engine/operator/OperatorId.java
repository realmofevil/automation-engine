// redundant?
package dev.realmofevil.automation.engine.operator;

import java.util.Objects;

public record OperatorId(String value) {
    public OperatorId {
        Objects.requireNonNull(value, "OperatorId must not be null");
    }

    @Override
    public String toString() {
        return value;
    }
}
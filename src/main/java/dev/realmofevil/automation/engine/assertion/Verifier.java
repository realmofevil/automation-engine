package dev.realmofevil.automation.engine.assertion;

import java.util.function.Consumer;

/**
 * Functional Interface for Composable Assertions.
 */
@FunctionalInterface
public interface Verifier<T> extends Consumer<T> {

    void verify(T actual);

    @Override
    default void accept(T t) {
        verify(t);
    }

    default Verifier<T> and(Verifier<T> other) {
        return actual -> {
            this.verify(actual);
            other.verify(actual);
        };
    }
}
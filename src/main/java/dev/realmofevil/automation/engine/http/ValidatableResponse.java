package dev.realmofevil.automation.engine.http;

import dev.realmofevil.automation.engine.reporting.StepReporter;
import org.junit.jupiter.api.Assertions;
import java.util.function.Consumer;

public class ValidatableResponse {
    private final Response response;

    public ValidatableResponse(Response response) {
        this.response = response;
    }

    public ValidatableResponse statusCode(int expectedCode) {
        if (response.status() != expectedCode) {
            String error = "Expected Status " + expectedCode + " but got " + response.status();
            StepReporter.error(error, null);
            Assertions.assertEquals(expectedCode, response.status(), error);
        }
        return this;
    }

    public ValidatableResponse assertOk() {
        if (response.status() >= 400) {
            String error = "Expected OK (2xx/3xx) but got " + response.status();
            StepReporter.error(error, null);
            Assertions.fail(error + "\nBody: " + response.raw().body());
        }
        return this;
    }

    /**
     * Extracts the body as a POJO and runs a custom assertion consumer.
     */
    public <T> ValidatableResponse verify(Class<T> type, Consumer<T> assertion) {
        T data = response.as(type);
        assertion.accept(data);
        return this;
    }

    /**
     * Terminating operation: Get the object.
     */
    public <T> T as(Class<T> type) {
        return response.as(type);
    }

    /**
     * Terminating operation: Get the raw Response wrapper.
     */
    public Response extract() {
        return response;
    }
}
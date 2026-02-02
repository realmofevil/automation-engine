package dev.realmofevil.automation.engine.http;

import com.fasterxml.jackson.databind.JsonNode;
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
            String error = String.format("Expected Status %d but got %d. Body: %s", expectedCode, response.status(),
                    response.raw().body());
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
     * Checks if the response body contains "success": true.
     * Common pattern in legacy APIs.
     */
    public ValidatableResponse assertSuccess() {
        assertOk();
        try {
            JsonNode root = response.mapper().readTree(response.raw().body());
            if (root.has("success")) {
                boolean success = root.get("success").asBoolean();
                if (!success) {
                    String msg = root.toString();
                    String error = "API returned logical failure (success: false).";
                    StepReporter.error(error, null);
                    Assertions.fail(error + " Body: " + msg);
                }
            } else {
                StepReporter.warn("Response body does not contain 'success' field. Skipping logical check.");
            }
        } catch (Exception e) {
        }
        return this;
    }

    /**
     * Replaces RestAssured: response.jsonPath().get("key")
     * Uses JSON Pointer syntax: "/data/token"
     */
    public String jsonPath(String path) {
        try {
            JsonNode root = response.mapper().readTree(response.raw().body());
            JsonNode node = root.at(path);
            if (node.isMissingNode()) {
                throw new IllegalArgumentException("JSON Path not found: " + path);
            }
            return node.asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract JSON path: " + path, e);
        }
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
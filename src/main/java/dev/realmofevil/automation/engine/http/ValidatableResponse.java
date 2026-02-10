package dev.realmofevil.automation.engine.http;

import com.fasterxml.jackson.databind.JsonNode;
import dev.realmofevil.automation.engine.reporting.StepReporter;
import org.junit.jupiter.api.Assertions;
import java.util.function.Consumer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

public class ValidatableResponse {
    private final Response response;

    public ValidatableResponse(Response response) {
        this.response = response;
    }

    public ValidatableResponse statusCode(int expectedCode) {
        if (response.status() != expectedCode) {
            String bodyPreview = getBodyPreview();
            String error = String.format("API FAILURE | Status: %d (Expected: %d) | Body: %s",
                    response.status(), expectedCode, bodyPreview);

            StepReporter.error(error, null);
            StepReporter.attachJson("Failure Response Body", response.raw().body());

            Assertions.assertEquals(expectedCode, response.status(), error);
        }
        return this;
    }

    public ValidatableResponse assertOk() {
        if (response.status() >= 400) {
            String bodyPreview = getBodyPreview();
            String error = String.format("API FAILURE | Status: %d | Body: %s",
                    response.status(), bodyPreview);

            StepReporter.error(error, null);
            StepReporter.attachJson("Failure Response Body", response.raw().body());

            Assertions.fail(error);
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
            if (root.isObject() && root.has("success")) {
                boolean success = root.get("success").asBoolean();
                if (!success) {
                    String error = "LOGICAL FAILURE | API returned \"success\": false | Body: " + getBodyPreview();
                    StepReporter.error(error, null);
                    Assertions.fail(error);
                }
            } else {
                StepReporter.warn("Response body does not contain a standard \"success\" field. Skipping logical check.");
            }
        } catch (Exception e) {
        }
        return this;
    }

    private String getBodyPreview() {
        String body = response.raw().body();
        if (body == null)
            return "null";
        return (body.length() > 200) ? body.substring(0, 200) + "... (see attachment)" : body;
    }

    /**
     * Replaces RestAssured: response.jsonPath().get("key")
     * Uses JSON Pointer syntax: "/data/token"
     */
    public String jsonPath(String path) {
        JsonNode root;
        try {
            root = response.mapper().readTree(response.raw().body());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response body as JSON. Body: " + getBodyPreview(), e);
        }

        JsonNode node = root.at(path);
        if (node.isMissingNode()) {
            throw new IllegalArgumentException(String.format("JSON Path '%s' not found in response. Body: %s", 
                path, getBodyPreview()));
        }
        return node.asText();
    }

    /**
     * Extracts a value from an XML response using XPath.
     * Example: xmlPath("//result/token")
     */
    public String xmlPath(String xpathExpression) {
        try {
            String body = response.raw().body();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(body)));

            XPath xpath = XPathFactory.newInstance().newXPath();
            return xpath.evaluate(xpathExpression, doc);
        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate XPath: " + xpathExpression, e);
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
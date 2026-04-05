package dev.realmofevil.automation.engine.http;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import dev.realmofevil.automation.engine.reporting.StepReporter;
import org.junit.jupiter.api.Assertions;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Validates and extracts data from an HTTP Response.
 * Implements robust JSON/XML handling, error translation, and logging sanitization.
 */
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
            StepReporter.attachText("Failure Response Body", response.raw().body());

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
            StepReporter.attachText("Failure Response Body", response.raw().body());

            Assertions.fail(error);
        }
        return this;
    }

    /**
     * Domain-Specific Check: Verifies the application returned {"success": true}.
     * Fails fast with clear messaging if the response is valid JSON but logically failed.
     */
    public ValidatableResponse assertSuccess() {
        assertOk();

        JsonNode root;
        try {
            root = response.mapper().readTree(response.raw().body());
        } catch (Exception e) {
            handleJsonParsingError(e, "assertSuccess");
            return this;
        }

        if (root.isObject() && root.has("success")) {
            if (!root.get("success").asBoolean()) {
                String error = "LOGICAL FAILURE | API returned \"success\": false | Body: " + getBodyPreview();
                StepReporter.error(error, null);
                Assertions.fail(error);
            }
        } else {
            StepReporter.warn("Response body does not contain a standard \"success\" field. Skipping logical check.");
        }
        return this;
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
            handleJsonParsingError(e, "jsonPath(" + path + ")");
            return null;
        }

        JsonNode node = root.at(path);
        if (node.isMissingNode()) {
            String error = String.format("JSON Path '%s' not found in response. Body: %s", path, getBodyPreview());
            StepReporter.error(error, null);
            throw new IllegalArgumentException(error);
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
            throw handleXmlParsingError(e, xpathExpression);
        }
    }

    /**
     * Deserializes the response into a Java Record/Class.
     */
    public <T> T as(Class<T> type) {
        try {
            return response.mapper().readValue(response.raw().body(), type);
        } catch (Exception e) {
            handleJsonParsingError(e, "deserialization to " + type.getSimpleName());
            return null;
        }
    }

    /**
     * Extracts the body as a POJO and runs a custom assertion consumer.
     */
    public <T> ValidatableResponse verify(Class<T> type, Consumer<T> assertion) {
        T data = as(type);
        assertion.accept(data);
        return this;
    }

    /**
     * Terminating operation: Get the raw Response wrapper.
     */
    public Response extract() {
        return response;
    }

    private String getBodyPreview() {
        String body = response.raw().body();
        if (body == null || body.isBlank()) return "<empty>";

        String cleanBody = body.replace("\r", "").replace("\n", " ").replace("\t", " ");
        return (cleanBody.length() > 200) ? cleanBody.substring(0, 180) + "... (see attachment)" : cleanBody;
    }

    /**
     * Translates raw Jackson exceptions into highly actionable automation errors.
     */
    private void handleJsonParsingError(Exception e, String context) {
        String body = response.raw().body();
        boolean isHtml = body != null && body.trim().toLowerCase(Locale.ROOT).startsWith("<html");

        String humanReadableMessage = switch (e) {
            case JsonParseException jpe when isHtml ->
                String.format("API FORMAT ERROR during %s: Expected JSON, but the server returned an HTML page (e.g., 502 Bad Gateway or WAF Block).", context);
            case MismatchedInputException mie when isHtml ->
                String.format("API FORMAT ERROR during %s: Expected JSON, but the server returned an HTML page (e.g., 502 Bad Gateway or WAF Block).", context);
            case JsonParseException jpe ->
                String.format("API FORMAT ERROR during %s: The server returned malformed JSON. Cannot parse response.", context);
            case MismatchedInputException mie ->
                String.format("SCHEMA MISMATCH during %s: The JSON returned by the API does not match the Java Data Model. (Check if an object was expected but an array was returned).", context);
            default ->
                String.format("UNEXPECTED ERROR during %s parsing: %s", context, e.getMessage());
        };

        StepReporter.error(humanReadableMessage, null);
        StepReporter.attachText("Unparsable Response Body", body);
        throw new RuntimeException(humanReadableMessage, e);
    }

    private RuntimeException handleXmlParsingError(Exception e, String context) {
        String body = response.raw().body();

        String humanReadableMessage = switch (e) {
            case SAXParseException spe ->
                String.format("XML FORMAT ERROR: Expected valid XML, but the server returned malformed content. Cannot parse response for XPath '%s'.", context);
            case XPathExpressionException xpe ->
                String.format("XPATH EVALUATION ERROR: The XPath expression '%s' is invalid or could not be evaluated against the response.", context);
            default ->
                String.format("UNEXPECTED ERROR during XML parsing for '%s': %s", context, e.getMessage());
        };

        StepReporter.error(humanReadableMessage, null);
        StepReporter.attachText("Unparsable XML Response Body", body);
        return new RuntimeException(humanReadableMessage, e);
    }
}
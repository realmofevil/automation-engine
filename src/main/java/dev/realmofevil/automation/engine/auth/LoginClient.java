package dev.realmofevil.automation.engine.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.NetworkExceptionTranslator;
import dev.realmofevil.automation.engine.reporting.SmartRedactor;
import dev.realmofevil.automation.engine.reporting.StepReporter;
import dev.realmofevil.automation.engine.routing.RouteDefinition;
import dev.realmofevil.automation.engine.security.CryptoUtil;
import dev.realmofevil.automation.engine.util.TemplateProcessor;
import io.qameta.allure.Allure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles application-level session acquisition (Login) and termination (Logout).
 * Bypasses the standard AuthenticationChain to prevent circular dependencies.
 */
public final class LoginClient {
    private final ExecutionContext context;
    private final ObjectMapper mapper;

    public LoginClient(ExecutionContext context) {
        this.context = context;
        this.mapper = new ObjectMapper();
    }

    public void login(OperatorConfig.ApiAccount account, OperatorConfig.AuthDefinition def) {
        String maskedUser = SmartRedactor.maskValue(account.username().plainText());
        Allure.step("Authenticating as " + maskedUser + " via " + def.type(), () -> {
            performLoginRequest(account, def);
        });
    }

    private void performLoginRequest(OperatorConfig.ApiAccount account, OperatorConfig.AuthDefinition def) {
		URI loginUri = null;
        try {
            String routePath = def.loginRoute();
            if (!routePath.startsWith("/")) {
                RouteDefinition routeDef = context.routes().get(routePath);
                routePath = routeDef.path();
            }
            loginUri = context.config().domains().desktopUri().resolve(routePath);

            Map<String, Object> globalDefaults = context.config().contextDefaults();
            Map<String, Object> accountRawMetadata = account.metadata() != null ? account.metadata() : new HashMap<>();

            Map<String, Object> effectiveMetadata = new HashMap<>(globalDefaults);
            effectiveMetadata.putAll(accountRawMetadata);

            Map<String, Object> decodedMetadata = new HashMap<>(globalDefaults);
            accountRawMetadata.forEach((k, v) -> {
                if (v instanceof String s) {
                    decodedMetadata.put(k, CryptoUtil.decodeBase64(s, true));
                } else {
                    decodedMetadata.put(k, v);
                }
            });

            Map<String, Object> resolutionContext = new HashMap<>();
            resolutionContext.put("context", globalDefaults);

            Map<String, Object> accountView = new HashMap<>();
            accountView.put("username", account.username().plainText());
            accountView.put("password", account.password().plainText());
            accountView.put("metadata", effectiveMetadata);
            accountView.put("decodedMetadata", decodedMetadata);

            resolutionContext.put("account", accountView);

            if (def.payloadTemplate() == null || def.payloadTemplate().isEmpty()) {
                throw new IllegalStateException("AuthDefinition missing 'payloadTemplate' in YAML.");
            }

            Object payloadObject = TemplateProcessor.process(def.payloadTemplate(), resolutionContext);
            String jsonBody = mapper.writeValueAsString(payloadObject);

            String referer = context.config().domains().desktopUri().toString();
            String userAgent = String.valueOf(globalDefaults.get("userAgent"));
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(loginUri)
                    .header("Content-Type", "application/json")
                    .header("Referer", referer)
                    .header("User-Agent", userAgent)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            String transportUser = context.auth().getTransportUser();
            String transportPass = context.auth().getTransportPassword();
            if (transportUser != null && transportPass != null) {
                String basicAuth = Base64.getEncoder().encodeToString(
                        (transportUser + ":" + transportPass).getBytes(StandardCharsets.UTF_8));
                requestBuilder.header("Authorization", "Basic " + basicAuth);
            }

            HttpRequest request = requestBuilder.build();

            HttpResponse<String> response;
            try {
                HttpClient client = context.api().getNativeClient(true);
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                throw NetworkExceptionTranslator.translate(e, loginUri, "Authentication Login Request");
            }

            StepReporter.attachJson("Login Response", response.body());

            String rawBody = response.body();
            String cleanBody = (rawBody == null || rawBody.isBlank()) 
                    ? "<empty>" 
                    : rawBody.replace("\r", "").replace("\n", " ").replace("\t", " ");

            int status = response.statusCode();
            if (status == 401 || status == 403) {
                throw new RuntimeException("AUTHENTICATION REJECTED: The server returned " + status + ". Check if the Base64 credentials in YAML are correct for user: " + account.username().plainText() + ". Body: " + cleanBody);
            }
            if (status == 502 || status == 503 || status == 504) {
                throw new RuntimeException("AUTH SERVICE DOWN: The Gateway returned " + status + ". The backend authentication service is currently unreachable.");
            }
            if (status >= 400) {
                throw new RuntimeException("LOGIN FAILED: Unexpected HTTP " + status + " | Body: " + cleanBody);
            }

            JsonNode root;
            try {
                root = mapper.readTree(response.body());
            } catch (Exception e) {
                throw new RuntimeException("LOGIN FORMAT ERROR: Expected JSON token response, but received HTML or malformed data. Body: " + cleanBody, e);
            }

            if (root.has("success") && !root.get("success").asBoolean()) {
                throw new RuntimeException("LOGIN LOGIC FAILED: API returned success=false. Credentials might be blocked or invalid. Body: " + cleanBody);
            }

            String token;
            try {
                token = extractToken(response, root, def);
            } catch (Exception e) {
                throw new RuntimeException("TOKEN EXTRACTION FAILED: Could not find token using field '" + def.tokenField() + "'. Has the API contract changed?", e);
            }

            if (token == null || token.isBlank()) {
                throw new RuntimeException("TOKEN EXTRACTION FAILED: Token field '" + def.tokenField() + "' was found but is empty, using source '" + def.tokenSource() + "'.");
            }

            context.auth().setAuthToken(token);
            String maskedToken = SmartRedactor.maskValue(token);
            StepReporter.info("Token acquired for header \"" + def.tokenField() + "\": " + maskedToken);
            Allure.addAttachment("Auth Success", "Token acquired for: " + SmartRedactor.maskValue(account.username().plainText()) + " (length: "
                    + token.length() + ", value: " + maskedToken + ")");

        } catch (RuntimeException e) {
            StepReporter.error("Authentication Flow Failed", null);
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Authentication Flow Failed due to unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Terminate session gracefully if supported by the operator context.
     */
    public void logout(OperatorConfig.ApiAccount account) {
        if (!context.auth().isAuthenticated())
            return;

        OperatorConfig.AuthDefinition authDef = context.config().auth().stream()
                .filter(d -> d.type() == OperatorConfig.AuthType.LOGIN_TOKEN)
                .findFirst()
                .orElse(null);

        if (authDef == null || authDef.logoutRoute() == null || authDef.logoutRoute().isBlank()) {
            return;
        }

        try {
            String routePath = authDef.logoutRoute();
            if (!routePath.startsWith("/")) {
                RouteDefinition routeDef = context.routes().get(routePath);
                routePath = routeDef.path();
            }
            URI logoutUri = context.config().domains().desktopUri().resolve(routePath);

            String referer = context.config().domains().desktopUri().toString();
            String userAgent = String.valueOf(context.config().contextDefaults().get("userAgent"));

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(logoutUri)
                    .header("Referer", referer)
                    .header("User-Agent", userAgent)
                    .GET();
            String transportUser = context.auth().getTransportUser();
            String transportPass = context.auth().getTransportPassword();
            if (transportUser != null && transportPass != null) {
                String basicAuth = Base64.getEncoder().encodeToString(
                        (transportUser + ":" + transportPass).getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + basicAuth);
            }

            String token = context.auth().getAuthToken();
            if (token != null) {
                String headerName = "Authorization";
                String headerValue = "Bearer " + token;

                if (authDef.tokenHeader() != null && !authDef.tokenHeader().isBlank()) {
                    headerName = authDef.tokenHeader();
                    if (!"Authorization".equalsIgnoreCase(headerName)) {
                        headerValue = token;
                    }
                }
                
                builder.header(headerName, headerValue);
            }

            context.api().getNativeClient(true).send(builder.build(), HttpResponse.BodyHandlers.discarding());

            String maskedUser = SmartRedactor.maskValue(account.username().plainText());
            StepReporter.info("Logged out user: " + maskedUser);

        } catch (Exception e) {
            StepReporter.warn("Logout failed (non-critical): " + e.getMessage());
        }
    }

    private String extractToken(HttpResponse<String> response, JsonNode root, OperatorConfig.AuthDefinition def) {
        if (def.tokenSource() == OperatorConfig.TokenSource.RESPONSE_HEADER) {
            return response.headers()
                    .firstValue(def.tokenField())
                    .orElseThrow(() -> new RuntimeException("Header not found: " + def.tokenField()));
        } else {
            String field = def.tokenField();
            if (field.contains(".")) {
                return root.at("/" + field.replace(".", "/")).asText();
            }
            return root.path(field).asText();
        }
    }
}
package dev.realmofevil.automation.engine.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.ApiRequestSpec;
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

public final class LoginClient {
    private final ExecutionContext context;
    private final ObjectMapper mapper;

    public LoginClient(ExecutionContext context) {
        this.context = context;
        this.mapper = new ObjectMapper();
    }

    public void login(OperatorConfig.ApiAccount account, OperatorConfig.AuthDefinition def) {
        Allure.step("Authenticating as " + account.username().plainText() + " via " + def.type(), () -> {
            performLoginRequest(account, def);
        });
    }

    private void performLoginRequest(OperatorConfig.ApiAccount account, OperatorConfig.AuthDefinition def) {
        try {
            String routePath = def.loginRoute();
            if (!routePath.startsWith("/")) {
                RouteDefinition routeDef = context.routes().get(routePath);
                routePath = routeDef.path();
            }

            URI loginUri = context.config().domains().desktopUri().resolve(routePath);

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

            HttpClient client = context.api().getNativeClient(true);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            StepReporter.attachJson("Login Response", response.body());

            if (response.statusCode() >= 400) {
                throw new RuntimeException(
                        "Login failed. Status: " + response.statusCode() + " Body: " + response.body());
            }

            JsonNode root = mapper.readTree(response.body());
            if (root.has("success") && !root.get("success").asBoolean()) {
                throw new RuntimeException("Login Logic Failed: " + response.body());
            }

            String token = extractToken(response, root, def);

            if (token == null || token.isBlank()) {
                throw new RuntimeException(
                        "Auth token not found using source: " + def.tokenSource() + " and field: " + def.tokenField());
            }

            context.auth().setAuthToken(token);
            String maskedToken = SmartRedactor.maskValue(token);
            StepReporter.info("Token acquired for header \"" + def.tokenField() + "\": " + maskedToken);
            Allure.addAttachment("Auth Success", "Token acquired for: " + account.username() + " (length: "
                    + token.length() + ", value: " + maskedToken + ")");

        } catch (Exception e) {
            throw new RuntimeException("Authentication flow failed: " + e.getMessage(), e);
        }
    }

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

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(logoutUri)
                    .GET();
            String referer = context.config().domains().desktopUri().toString();
            builder.header("Referer", referer);

            String transportUser = context.auth().getTransportUser();
            String transportPass = context.auth().getTransportPassword();
            if (transportUser != null && transportPass != null) {
                String basicAuth = Base64.getEncoder().encodeToString(
                        (transportUser + ":" + transportPass).getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + basicAuth);
            }

            String token = context.auth().getAuthToken();
            if (token != null && authDef.tokenHeader() != null) {
                builder.header(authDef.tokenHeader(), token);
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
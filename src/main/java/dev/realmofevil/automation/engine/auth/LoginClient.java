package dev.realmofevil.automation.engine.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.routing.RouteDefinition;
import dev.realmofevil.automation.engine.security.CryptoUtil;
import dev.realmofevil.automation.engine.util.TemplateProcessor;
import io.qameta.allure.Allure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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

            Map<String, Object> resolutionContext = new HashMap<>();
            resolutionContext.put("context", context.config().contextDefaults());

            Map<String, Object> accountData = new HashMap<>();
            accountData.put("username", account.username().plainText());
            accountData.put("password", account.password().plainText());
            accountData.put("metadata", account.metadata());

            Map<String, Object> decodedMetadata = new HashMap<>();
            if (account.metadata() != null) {
                account.metadata().forEach((k, v) -> {
                    if (v instanceof String s) {
                        decodedMetadata.put(k, CryptoUtil.decodeBase64(s, true));
                    } else {
                        decodedMetadata.put(k, v);
                    }
                });
            }
            accountData.put("decodedMetadata", decodedMetadata);

            resolutionContext.put("account", accountData);

            if (def.payloadTemplate() == null || def.payloadTemplate().isEmpty()) {
                throw new IllegalStateException("AuthDefinition missing 'payloadTemplate' in YAML.");
            }

            Object payloadObject = TemplateProcessor.process(def.payloadTemplate(), resolutionContext);
            String jsonBody = mapper.writeValueAsString(payloadObject);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(loginUri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpClient client = context.api().getNativeClient(true);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException(
                        "Login failed. Status: " + response.statusCode() + " Body: " + response.body());
            }

            String token = extractToken(response, def);
            if (token == null || token.isBlank()) {
                throw new RuntimeException(
                        "Auth token not found using source: " + def.tokenSource() + " and field: " + def.tokenField());
            }

            context.auth().setAuthToken(token);
            Allure.addAttachment("Auth Success", "Token acquired for: " + account.username() + " " + token.length());

        } catch (Exception e) {
            throw new RuntimeException("Authentication flow failed", e);
        }
    }

    private String extractToken(HttpResponse<String> response, OperatorConfig.AuthDefinition def) throws Exception {
        if (def.tokenSource() == OperatorConfig.TokenSource.RESPONSE_HEADER) {
            return response.headers()
                    .firstValue(def.tokenField())
                    .orElseThrow(() -> new RuntimeException("Header not found: " + def.tokenField()));
        } else {
            JsonNode root = mapper.readTree(response.body());
            String field = def.tokenField();
            if (field.contains(".")) {
               return root.at("/" + field.replace(".", "/")).asText();
            }
            return root.path(field).asText();
        }
    }
}
package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.ApiRequest;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class BasicAuthenticationStep implements AuthenticationStep {

    @Override
    public ApiRequest apply(ExecutionContext context, ApiRequest request) {
        String username = context.auth().getTransportUser();
        String password = context.auth().getTransportPassword();

        if (username == null || password == null) {
            return request;
        }

        HttpRequest original = request.httpRequest();
        URI finalUri = original.uri();
        Map<String, String> authHeaders = new HashMap<>();

        boolean useUrlAuth = false;
        if (context.config().auth() != null) {
            for (OperatorConfig.AuthDefinition def : context.config().auth()) {
                if (def.type() == OperatorConfig.AuthType.BASIC_URL) {
                    useUrlAuth = true;
                    break;
                }
            }
        }

        if (useUrlAuth) {
            try {
                String userInfo = username + ":" + password;
                finalUri = new URI(
                        original.uri().getScheme(),
                        userInfo,
                        original.uri().getHost(),
                        original.uri().getPort(),
                        original.uri().getPath(),
                        original.uri().getQuery(),
                        null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject Basic Auth into URL", e);
            }
        } else {
            String basicAuth = Base64.getEncoder().encodeToString(
                    (username + ":" + password).getBytes(StandardCharsets.UTF_8));
            authHeaders.put("Authorization", "Basic " + basicAuth);
        }

        ApiRequest.Builder newBuilder = ApiRequest.builder()
                .uri(finalUri)
                .method(original.method(), original.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));

        original.headers().map().forEach((k, v) -> {
            v.forEach(val -> newBuilder.headers(Map.of(k, val)));
        });

        newBuilder.headers(authHeaders);

        return newBuilder.build();
    }
}
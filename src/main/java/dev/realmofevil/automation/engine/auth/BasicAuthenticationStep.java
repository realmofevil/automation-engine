package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.ApiRequest;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public final class BasicAuthenticationStep implements AuthenticationStep {

    @Override
    public ApiRequest apply(ExecutionContext context, ApiRequest request) {

        String username = context.auth().getTransportUser();
        String password = context.auth().getTransportPassword();

        if (username == null || password == null) {
            return request;
        }

        String basicAuth = Base64.getEncoder().encodeToString(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));

        HttpRequest original = request.httpRequest();
        URI uri = original.uri();

        URI uriWithCreds;
        try {
            String userInfo = username + ":" + password;
            uriWithCreds = new URI(
                    uri.getScheme(),
                    userInfo,
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to construct Basic Auth URL", e);
        }

        return ApiRequest.builder()
                .uri(uriWithCreds)
                .headers(Map.of("Authorization", "Basic " + basicAuth))
                .method(
                        original.method(),
                        original.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()))
                .build();
    }
}
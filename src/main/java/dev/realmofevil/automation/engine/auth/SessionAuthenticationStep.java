package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.ApiRequest;

import java.net.http.HttpRequest;
import java.util.Map;

public final class SessionAuthenticationStep implements AuthenticationStep {

    @Override
    public ApiRequest apply(ExecutionContext context, ApiRequest request, AuthContext authContext) {

        String cookieHeader = context.auth().getCookieHeader();

        if (cookieHeader == null || cookieHeader.isBlank()) {
            return request;
        }

        HttpRequest original = request.httpRequest();

        return ApiRequest.builder()
                .uri(original.uri())
                .headers(Map.of("Cookie", cookieHeader))
                .method(
                        original.method(),
                        original.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody())
                )
                .build();
    }
}
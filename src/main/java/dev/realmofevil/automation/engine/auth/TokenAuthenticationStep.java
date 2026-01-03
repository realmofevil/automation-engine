package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.ApiRequest;

import java.net.http.HttpRequest;
import java.util.Map;

public final class TokenAuthenticationStep
        implements AuthenticationStep {

    @Override
    public ApiRequest apply(
            ExecutionContext context,
            ApiRequest request,
            AuthContext authContext
    ) {
        String token =
                authContext.get("token");

        if (token == null) {
            throw new IllegalStateException(
                    "Token not available in AuthContext"
            );
        }

        HttpRequest original = request.httpRequest();

        return ApiRequest.builder()
                .uri(original.uri())
                .headers(
                        Map.of(
                                "Authorization",
                                "Bearer " + token
                        )
                )
                .method(
                        original.method(),
                        original.bodyPublisher().orElse(
                                HttpRequest.BodyPublishers.noBody()
                        )
                )
                .build();
    }
}
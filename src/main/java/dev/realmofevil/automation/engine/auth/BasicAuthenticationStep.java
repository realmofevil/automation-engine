package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.ApiRequest;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class BasicAuthenticationStep
        implements AuthenticationStep {

    @Override
    public ApiRequest apply(
            ExecutionContext context,
            ApiRequest request,
            AuthContext authContext
    ) {
        AccountCredentials account =
                context.accounts().next();

        String basic =
                Base64.getEncoder().encodeToString(
                        (account.username() + ":" + account.password())
                                .getBytes(StandardCharsets.UTF_8)
                );

        HttpRequest original = request.httpRequest();
        URI uri = original.uri();

        URI uriWithCreds =
                URI.create(
                        uri.getScheme() + "://"
                                + account.username()
                                + ":" + account.password()
                                + "@"
                                + uri.getHost()
                                + uri.getRawPath()
                );

        return ApiRequest.builder()
                .uri(uriWithCreds)
                .headers(
                        java.util.Map.of(
                                "Authorization",
                                "Basic " + basic
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
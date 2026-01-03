package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.ApiClient;
import dev.realmofevil.automation.engine.http.ApiRequest;

import java.net.http.HttpRequest;
import java.util.Map;

public final class SessionAuthenticationStep
        implements AuthenticationStep {

    private final AccountSessionRegistry registry;

    public SessionAuthenticationStep(
            AccountSessionRegistry registry
    ) {
        this.registry = registry;
    }

    @Override
    public ApiRequest apply(
            ExecutionContext context,
            ApiRequest request,
            AuthContext authContext
    ) {
        AccountCredentials account =
                context.accounts().current();

        SessionState session =
                registry.sessionFor(account);

        if (!session.has("sessionToken")) {
            synchronized (session) {
                if (!session.has("sessionToken")) {
                    ApiClient client =
                            ApiClient.from(context);

                    LoginClient loginClient =
                            new LoginClient(
                                    client,
                                    context.operator()
                                            .loginEndpoint()
                            );

                    String token =
                            loginClient.login(account);

                    session.put("sessionToken", token);
                }
            }
        }

        HttpRequest original = request.httpRequest();

        return ApiRequest.builder()
                .uri(original.uri())
                .headers(
                        Map.of(
                                "X-Session-Token",
                                session.get("sessionToken")
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
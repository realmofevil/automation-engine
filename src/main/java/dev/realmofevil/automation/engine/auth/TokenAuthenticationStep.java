package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.ApiRequest;

import java.net.http.HttpRequest;
import java.util.Map;

public final class TokenAuthenticationStep implements AuthenticationStep {

    @Override
    public ApiRequest apply(ExecutionContext context, ApiRequest request) {
        String token = context.auth().getAuthToken();

        if (token == null) {
            return request;
        }

        String headerName = "Authorization"; 
        String headerValue = "Bearer " + token;

        if (context.config().auth() != null) {
            for (OperatorConfig.AuthDefinition def : context.config().auth()) {
                if (def.type() == OperatorConfig.AuthType.LOGIN_TOKEN) {
                    if (def.tokenHeader() != null && !def.tokenHeader().isBlank()) {
                        headerName = def.tokenHeader();
                        if (!"Authorization".equalsIgnoreCase(headerName)) {
                            headerValue = token;
                        }
                    }
                    break;
                }
            }
        }

        HttpRequest original = request.httpRequest();

        return ApiRequest.builder()
                .uri(original.uri())
                .headers(Map.of(headerName, headerValue))
                .method(
                        original.method(),
                        original.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody())
                )
                .build();
    }
}
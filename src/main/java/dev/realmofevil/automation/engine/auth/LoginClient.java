package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.http.ApiClient;
import dev.realmofevil.automation.engine.http.ApiRequest;
import dev.realmofevil.automation.engine.http.ApiResponse;

import java.net.URI;
import java.net.http.HttpRequest;

public final class LoginClient {

    private final ApiClient apiClient;
    private final URI loginEndpoint;

    public LoginClient(ApiClient apiClient, URI loginEndpoint) {
        this.apiClient = apiClient;
        this.loginEndpoint = loginEndpoint;
    }

    public String login(AccountCredentials account) {
        ApiRequest request =
                ApiRequest.builder()
                        .uri(loginEndpoint)
                        .method(
                                "POST",
                                HttpRequest.BodyPublishers.ofString(
                                        "{\"username\":\""
                                                + account.username()
                                                + "\",\"password\":\""
                                                + account.password()
                                                + "\"}"
                                )
                        )
                        .headers(
                                java.util.Map.of(
                                        "Content-Type",
                                        "application/json"
                                )
                        )
                        .build();

        ApiResponse response = apiClient.send(request);

        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Login failed for account: " + account.id()
            );
        }

        //TODO: extraction – parse JSON
        return response.body();
    }
}
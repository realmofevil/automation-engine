package dev.realmofevil.automation.engine.http;

import dev.realmofevil.automation.engine.context.ExecutionContext;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

public final class ApiClient {

    private final HttpClient client;

    public ApiClient(HttpClient client) {
        this.client = client;
    }

    public ApiResponse send(ApiRequest request) {
        try {
            HttpResponse<String> response =
                    client.send(
                            request.httpRequest(),
                            HttpResponse.BodyHandlers.ofString()
                    );
            return new ApiResponse(response);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("HTTP request failed", e);
        }
    }

    public static ApiClient from(ExecutionContext context) {
        return new ApiClient(context.httpClient());
    }
}
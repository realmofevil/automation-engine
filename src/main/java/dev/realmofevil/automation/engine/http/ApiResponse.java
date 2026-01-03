package dev.realmofevil.automation.engine.http;

import java.net.http.HttpResponse;

public final class ApiResponse {

    private final HttpResponse<String> response;

    public ApiResponse(HttpResponse<String> response) {
        this.response = response;
    }

    public int statusCode() {
        return response.statusCode();
    }

    public String body() {
        return response.body();
    }

    public HttpResponse<String> raw() {
        return response;
    }
}

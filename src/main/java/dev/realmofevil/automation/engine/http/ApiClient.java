package dev.realmofevil.automation.engine.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.realmofevil.automation.engine.context.ExecutionContext;

import java.net.http.HttpClient;
import java.time.Duration;

public class ApiClient {

    private final ExecutionContext context;
    private final ObjectMapper mapper;
    private final HttpClient defaultClient;
    private final HttpClient noRedirectClient;

    public ApiClient(ExecutionContext context) {
        this.context = context;
        this.mapper = new ObjectMapper();

        this.defaultClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.noRedirectClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public ApiRequestSpec route(String routeKey) {
        return new ApiRequestSpec(context, routeKey, this);
    }

    // --- Delegates to Fluent API ---

    public Response post(String routeKey, Object body) {
        return route(routeKey).post(body);
    }

    public Response get(String routeKey) {
        return route(routeKey).get();
    }

    // --- Accessors ---

    public HttpClient getNativeClient(boolean followRedirects) {
        return followRedirects ? defaultClient : noRedirectClient;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
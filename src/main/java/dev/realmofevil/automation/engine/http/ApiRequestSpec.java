package dev.realmofevil.automation.engine.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.reporting.StepReporter;
import dev.realmofevil.automation.engine.routing.RouteDefinition;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fluent Builder for API Requests.
 */
public class ApiRequestSpec {

    private final ExecutionContext context;
    private final String routeKey;
    private final Map<String, String> queryParams = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private final ApiClient apiClient;

    private boolean isRetry = false;
    private boolean followRedirects = true;

    public ApiRequestSpec(ExecutionContext context, String routeKey, ApiClient apiClient) {
        this.context = context;
        this.routeKey = routeKey;
        this.apiClient = apiClient;
    }

    public ApiRequestSpec query(String key, String value) {
        this.queryParams.put(key, value);
        return this;
    }

    public ApiRequestSpec header(String key, String value) {
        this.headers.put(key, value);
        return this;
    }

    public ApiRequestSpec noRedirects() {
        this.followRedirects = false;
        return this;
    }

    public Response get() {
        return execute("GET", null);
    }

    public Response post(Object body) {
        return execute("POST", body);
    }

    public Response put(Object body) {
        return execute("PUT", body);
    }

    public Response delete() {
        return execute("DELETE", null);
    }

    private Response execute(String method, Object body) {
        try {

            RouteDefinition routeDef = context.routes().get(routeKey);
            
            // --- SERVICE RESOLUTION ---
            // If the route definition specifies a service (e.g. "payment"), use that host.
            // Otherwise default to Desktop.
            // TODO: Maybe will have to update RouteDefinition to support this, or assume a convention.
            // Convention: If route key starts with "payment.", look for "payment" service.

            URI baseUri;
            if (routeKey.startsWith("payment.")) {
                baseUri = context.config().getServiceUri("payment");
            } else if (routeKey.startsWith("aux.")) {
                baseUri = context.config().getServiceUri("aux");
            } else {
                baseUri = context.config().domains().desktopUri();
            }

            String basePath = baseUri.getPath();
            if (!basePath.endsWith("/")) basePath += "/";
            
            String routePath = routeDef.path();
            if (routePath.startsWith("/")) routePath = routePath.substring(1);
            
            String fullPath = basePath + routePath;

            // String basePath = baseUri.getPath().endsWith("/") ? baseUri.getPath() : baseUri.getPath() + "/";
            // String routePath = routeDef.path().startsWith("/") ? routeDef.path().substring(1) : routeDef.path();
            // URI fullUri = baseUri.resolve(basePath + routePath);

            URI fullUri = new URI(
                baseUri.getScheme(), 
                baseUri.getUserInfo(), 
                baseUri.getHost(), 
                baseUri.getPort(), 
                fullPath, 
                baseUri.getQuery(), 
                baseUri.getFragment()
            );

            if (!queryParams.isEmpty()) {
                String query = queryParams.entrySet().stream()
                        .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + 
                                  URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                        .collect(Collectors.joining("&"));
                
                String existingQuery = fullUri.getQuery();
                String newQuery = existingQuery == null ? query : existingQuery + "&" + query;
                
                fullUri = new URI(fullUri.getScheme(), fullUri.getUserInfo(), fullUri.getHost(), fullUri.getPort(), fullUri.getPath(), newQuery, fullUri.getFragment());
            }

            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(fullUri);

            headers.forEach(builder::header);

            ObjectMapper mapper = apiClient.getMapper();
            if (body != null) {
                String json = mapper.writeValueAsString(body);
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(json));
                StepReporter.attachJson("Request Body", json);
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            ApiRequest initialRequest = new ApiRequest(builder.build());

            ApiRequest authenticatedRequest = context.authChain().apply(context, initialRequest);

            URI finalUri = authenticatedRequest.httpRequest().uri();

            if (!isRetry) {
                StepReporter.info(method + " " + finalUri);
            } else {
                StepReporter.warn("RETRY: " + method + " " + finalUri);
            }

            HttpClient client = apiClient.getNativeClient(followRedirects);
            HttpResponse<String> httpRes = client.send(authenticatedRequest.httpRequest(), HttpResponse.BodyHandlers.ofString());

            if (httpRes.statusCode() == 401 && !isRetry) {
                StepReporter.warn("401 Unauthorized. Refreshing token...");
                context.authManager().reauthenticate();
                this.isRetry = true; 
                return execute(method, body);
            }

            StepReporter.attachJson("Response " + httpRes.statusCode(), httpRes.body());
            
            return new Response(httpRes, mapper);

        } catch (Exception e) {
            throw new RuntimeException("Execution of API Request Failed: " + routeKey, e);
        }
    }
}
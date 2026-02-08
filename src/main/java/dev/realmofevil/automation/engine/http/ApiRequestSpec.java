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
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Fluent Builder for API Requests.
 * Supports both Route-Key based (Configuration) and Raw URL (Dynamic) execution.
 * 
 * Resiliency logic with Retries for Transient Errors and Auth Refresh:
 *  - Retries on 502, 503, 504 status codes with Exponential Backoff and Jitter.
 *  - Limits retries to a maximum count to avoid infinite loops.
 *  - Refreshes authentication tokens on 401
 *  - Logs retry attempts with context for easier debugging.
 * 
 * Security Enhancements:
 *  - Path parameters are URL-encoded before injection to prevent manipulation of the URL structure.
 *  - Query parameters are also URL-encoded to ensure safe transmission.
 *  - Default context parameters are auto-injected into paths securely.
 */
public class ApiRequestSpec {
    private final ExecutionContext context;
    private final String routeKey;
    private final URI rawUri;
    private final Map<String, String> pathParams = new HashMap<>();
    private final Map<String, String> queryParams = new LinkedHashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private final ApiClient apiClient;
    private boolean followRedirects = true;

    private static final int MAX_RETRIES = 3;
    private static final Set<Integer> RETRYABLE_STATUS_CODES = Set.of(502, 503, 504);

    public ApiRequestSpec(ExecutionContext context, String routeKey, ApiClient apiClient) {
        this.context = context;
        this.routeKey = routeKey;
        this.rawUri = null;
        this.apiClient = apiClient;
    }

    public ApiRequestSpec(ExecutionContext context, URI rawUri, ApiClient apiClient) {
        this.context = context;
        this.routeKey = "RAW_REQUEST";
        this.rawUri = rawUri;
        this.apiClient = apiClient;
    }

    public ApiRequestSpec pathParam(String key, String value) {
        this.pathParams.put(key, value);
        return this;
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

    public ValidatableResponse get() {
        return new ValidatableResponse(executeWithRetry("GET", null, 0));
    }

    public ValidatableResponse post(Object body) {
        return new ValidatableResponse(executeWithRetry("POST", body, 0));
    }

    public ValidatableResponse put(Object body) {
        return new ValidatableResponse(executeWithRetry("PUT", body, 0));
    }

    public ValidatableResponse delete() {
        return new ValidatableResponse(executeWithRetry("DELETE", null, 0));
    }

    /**
     * Sends a POST request with Content-Type: application/x-www-form-urlencoded.
     */
    public ValidatableResponse postForm(Map<String, String> params) {
        String formBody = params.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                        URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        this.headers.put("Content-Type", "application/x-www-form-urlencoded");
        return new ValidatableResponse(executeWithRetry("POST", formBody, 0));
    }

    /**
     * Internal execution loop handling Retries (Auth Refresh & Transient Errors).
     */
    private Response executeWithRetry(String method, Object body, int attempt) {
        try {
            URI targetUri = buildUri();
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(targetUri);
            headers.forEach(builder::header);

            ObjectMapper mapper = apiClient.getMapper();
            if (body != null) {
                String bodyString;
                if (body instanceof String) {
                    bodyString = (String) body;
                } else {
                    bodyString = apiClient.getMapper().writeValueAsString(body);
                    if (!headers.containsKey("Content-Type")) {
                        builder.header("Content-Type", "application/json");
                    }
                }
                
                builder.method(method, HttpRequest.BodyPublishers.ofString(bodyString));

                if (attempt == 0) {
                    String ct = headers.getOrDefault("Content-Type", "application/json");
                    if (ct.contains("json")) {
                        StepReporter.attachJson("Request Body", bodyString);
                    } else {
                        StepReporter.attachText("Request Body", bodyString);
                    }
                }
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            ApiRequest initialRequest = new ApiRequest(builder.build());
            ApiRequest authenticatedRequest = context.authChain().apply(context, initialRequest);
            URI finalUri = authenticatedRequest.httpRequest().uri();

            if (attempt == 0) {
                StepReporter.info(String.format("Invoking: %s [%s %s]", routeKey, method, finalUri));
                
                StringBuilder headerLog = new StringBuilder();
                authenticatedRequest.httpRequest().headers().map().forEach((k, v) -> 
                    headerLog.append(String.format("  %s: %s\n", k, String.join(", ", v)))
                );
                StepReporter.attachText("Request Headers", headerLog.toString());
            } else {
                StepReporter.warn(String.format("RETRY [%d/%d]: %s", attempt, MAX_RETRIES, routeKey));
            }

            HttpClient client = apiClient.getNativeClient(followRedirects);
            HttpResponse<String> httpRes = client.send(authenticatedRequest.httpRequest(),
                    HttpResponse.BodyHandlers.ofString());

            int status = httpRes.statusCode();

            if (status == 401 && attempt < 1) {
                StepReporter.warn("401 Unauthorized. Refreshing token...");
                context.authManager().reauthenticate();
                return executeWithRetry(method, body, attempt + 1);
            }

            if (RETRYABLE_STATUS_CODES.contains(status) && attempt < MAX_RETRIES) {
                long backoffMillis = (long) (Math.pow(2, attempt) * 1000) + ThreadLocalRandom.current().nextInt(500);
                StepReporter.warn(String.format("Transient Error %d. Backing off for %dms...", status, backoffMillis));
                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during backoff", e);
                }
                return executeWithRetry(method, body, attempt + 1);
            }

            StepReporter.attachJson("Response " + status, httpRes.body());
            return new Response(httpRes, mapper);

        } catch (Exception e) {
            throw new RuntimeException("API Execution Failed for: " + routeKey, e);
        }
    }

    /**
     * Constructs the target URI with Secure Path Param Injection.
     */
    private URI buildUri() throws Exception {
        URI targetUri;

        if (rawUri != null) {
            targetUri = rawUri;
        } else {
            RouteDefinition routeDef = context.routes().get(routeKey);
            String routePath = routeDef.path();

            Map<String, Object> defaults = context.config().contextDefaults();
            for (Map.Entry<String, Object> entry : defaults.entrySet()) {
                String key = entry.getKey();

                if (this.pathParams.containsKey(key)) {
                    continue; 
                }

                String token = "{" + key + "}";
                if (routePath.contains(token)) {
                    routePath = routePath.replace(token, String.valueOf(entry.getValue()));
                }
            }

            URI baseUri = context.config().domains().desktopUri();
            Map<String, String> services = context.config().services();
            if (services != null) {
                for (String serviceName : services.keySet()) {
                    if (routeKey.startsWith(serviceName + ".")) {
                        baseUri = context.config().getServiceUri(serviceName);
                        break;
                    }
                }
            }

            for (Map.Entry<String, String> entry : this.pathParams.entrySet()) {
                String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                routePath = routePath.replace("{" + entry.getKey() + "}", encodedValue);
            }

            String basePath = baseUri.getPath();
            if (!basePath.endsWith("/")) basePath += "/";
            if (routePath.startsWith("/")) routePath = routePath.substring(1);
            String fullPath = basePath + routePath;

            targetUri = new URI(
                    baseUri.getScheme(),
                    baseUri.getUserInfo(),
                    baseUri.getHost(),
                    baseUri.getPort(),
                    fullPath,
                    baseUri.getQuery(),
                    baseUri.getFragment());
        }

        if (!queryParams.isEmpty()) {
            String query = queryParams.entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                            URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            String existingQuery = targetUri.getQuery();
            String newQuery = existingQuery == null ? query : existingQuery + "&" + query;

            targetUri = new URI(
                    targetUri.getScheme(),
                    targetUri.getUserInfo(),
                    targetUri.getHost(),
                    targetUri.getPort(),
                    targetUri.getPath(),
                    newQuery,
                    targetUri.getFragment()
            );
        }

        return targetUri;
    }
}
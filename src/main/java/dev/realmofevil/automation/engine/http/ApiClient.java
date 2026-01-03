package dev.realmofevil.automation.engine.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

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

    public Response post(String routeKey, Object body) {
        return route(routeKey).post(body);
    }
    
    public Response get(String routeKey) {
        return route(routeKey).get();
    }

    public HttpClient getNativeClient(boolean followRedirects) { 
        return followRedirects ? defaultClient : noRedirectClient; 
    }
    
    public ObjectMapper getMapper() { return mapper; }

    public URI applyAuthentication(HttpRequest.Builder builder, URI originalUri) {
        List<OperatorConfig.AuthDefinition> authDefs = context.config().auth();
        if (authDefs == null || authDefs.isEmpty()) {
            return originalUri;
        }

        URI resultingUri = originalUri;
        
        String basicUser = context.auth().getTransportUser();
        String basicPass = context.auth().getTransportPassword();
        String authToken = context.auth().getAuthToken();

        for (OperatorConfig.AuthDefinition def : authDefs) {
            switch (def.type()) {
                case BASIC_HEADER:
                    if (basicUser != null && basicPass != null) {
                        String auth = basicUser + ":" + basicPass;
                        String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                        builder.header("Authorization", "Basic " + encoded);
                    }
                    break;

                case BASIC_URL:
                    if (basicUser != null && basicPass != null) {
                        try {
                            String userInfo = basicUser + ":" + basicPass;
                            resultingUri = new URI(
                                originalUri.getScheme(), 
                                userInfo, 
                                originalUri.getHost(), 
                                originalUri.getPort(), 
                                originalUri.getPath(), 
                                originalUri.getQuery(), 
                                null
                            );
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to construct Basic Auth URL", e);
                        }
                    }
                    break;

                case LOGIN_TOKEN:
                    if (authToken != null) {
                        String headerName = def.tokenHeader();
                        if (headerName == null || headerName.equalsIgnoreCase("Authorization")) {
                            builder.header("Authorization", "Bearer " + authToken);
                        } else {
                            builder.header(headerName, authToken);
                        }
                    }
                    break;

                case SESSION_COOKIE:
                    String cookieHeader = context.auth().getCookieHeader();
                    if (cookieHeader != null) {
                        builder.header("Cookie", cookieHeader);
                    }
                    break;
            }
        }
        return resultingUri;
    }
}
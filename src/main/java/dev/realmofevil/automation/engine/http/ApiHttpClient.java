package dev.realmofevil.automation.engine.http;

import dev.realmofevil.automation.engine.auth.AuthContext;
import dev.realmofevil.automation.engine.auth.AuthSession;
import dev.realmofevil.automation.engine.config.AuthConfig;
import dev.realmofevil.automation.engine.config.AuthMechanism;
import dev.realmofevil.automation.engine.context.ExecutionContext;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

public final class ApiHttpClient {

    private ApiHttpClient() {}

     /** TODO:
      * better to have execution handled elsewhere (tests, services, executors)
      * implement retries, logging, Allure reporting based on HttpRequest and HttpResponse?
      * implement logging of request/response bodies?
     **/
    public static HttpRequest.Builder request(String path) {
        
        // OperatorEndpoint endpoint = ExecutionContext.endpoint();
        // thread safety race condition if another thread is called between operator() and environment() and non-atomic changes occur?
        // current thread's operator endpoint
        String operator = ExecutionContext.operator();
        var endpoint = ExecutionContext.environment()
                .operators()
                .get(operator);

        URI uri = URI.create(endpoint.baseUrl() + path);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(
                        ExecutionContext.environment().timeoutMs()));

        return applyAuthentication(builder, endpoint.auth());
    }

    private static HttpRequest.Builder applyAuthentication(
            HttpRequest.Builder builder,
            AuthConfig authConfig) {

        if (authConfig == null)
            return builder;

        AuthSession session = AuthContext.get();
        if (session == null)
            return builder;

        if (authConfig.mechanisms().contains(AuthMechanism.BASIC)) {
            builder.header(
                    "Authorization",
                    "Basic " + encodeBasic(session));
        }

        if (authConfig.mechanisms().contains(AuthMechanism.TOKEN)
                && session.token() != null) {
            builder.header(
                    "Authorization",
                    "Bearer " + session.token());
        }

        if (authConfig.mechanisms().contains(AuthMechanism.SESSION)
                && !session.cookies().isEmpty()) {
            builder.header(
                    "Cookie",
                    serializeCookies(session.cookies()));
        }

        return builder;
    }

    private static String encodeBasic(AuthSession session) {
        return Base64.getEncoder()
                .encodeToString(
                        (session.username() + ":" + session.password())
                                .getBytes(StandardCharsets.UTF_8));
    }

    private static String serializeCookies(Map<String, String> cookies) {
        return cookies.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("; "));
    }
}

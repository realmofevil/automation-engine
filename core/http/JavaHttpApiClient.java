package core.http;

import domain.request.ApiRequest;
import domain.response.ApiResponse;

import java.net.http.*;
import java.net.URI;
import java.time.Duration;

public final class JavaHttpApiClient implements ApiClient {

    private final HttpClient client;
    private final URI baseUrl;

    public JavaHttpApiClient(URI baseUrl, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    @Override
    public ApiResponse execute(ApiRequest request) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(baseUrl.resolve(request.endpoint()))
                    .method(request.method(), request.bodyPublisher());

            request.headers().forEach(builder::header);

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new ApiResponse(response.statusCode(), response.headers().map(), response.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
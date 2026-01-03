package dev.realmofevil.automation.engine.http;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

public final class ApiRequest {

    private final HttpRequest request;

    public ApiRequest(HttpRequest request) {
        this.request = request;
    }

    public HttpRequest httpRequest() {
        return request;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .timeout(Duration.ofSeconds(30));

        public Builder uri(URI uri) {
            builder.uri(uri);
            return this;
        }

        public Builder method(
                String method,
                HttpRequest.BodyPublisher body
        ) {
            builder.method(method, body);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            headers.forEach(builder::header);
            return this;
        }

        public ApiRequest build() {
            return new ApiRequest(builder.build());
        }
    }
}
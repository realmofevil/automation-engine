package domain.request;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;

public record ApiRequest(
        String method,
        URI endpoint,
        Map<String, String> headers,
        String body
) {
    public HttpRequest.BodyPublisher bodyPublisher() {
        return body == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(body);
    }
}
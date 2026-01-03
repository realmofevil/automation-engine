
package dev.realmofevil.automation.engine.http;

import java.net.http.HttpClient;
import java.time.Duration;

public final class HttpClientProvider {
    public static HttpClient create() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
}
package dev.realmofevil.automation.engine.http;

import io.qameta.allure.Allure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {

    private final HttpClient client = HttpClient.newHttpClient();

    public HttpResponse<String> get(String url) throws Exception {
        return send("GET", url, null);
    }

    public HttpResponse<String> post(String url, String body) throws Exception {
        return send("POST", url, body);
    }

    private HttpResponse<String> send(String method, String url, String body)
            throws Exception {

        Allure.step(method + " " + url);

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json");

        if (body != null) {
            // if ("POST".equals(method)) {
            Allure.addAttachment("Request Body", body);
            b.POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            b.GET();
        }

        HttpResponse<String> response = client.send(
                b.build(),
                HttpResponse.BodyHandlers.ofString());

        Allure.addAttachment(
                "Response (" + response.statusCode() + ")",
                response.body());

        return response;
    }
}

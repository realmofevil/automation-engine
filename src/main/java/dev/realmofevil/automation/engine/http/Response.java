package dev.realmofevil.automation.engine.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpResponse;

public record Response(HttpResponse<String> raw, ObjectMapper mapper) {
    
    public int status() { return raw.statusCode(); }
    
    public <T> T as(Class<T> type) {
        try {
            return mapper.readValue(raw.body(), type);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize response", e);
        }
    }
    
    public void assertOk() {
        if (status() >= 400) {
            throw new AssertionError("Expected OK status, got " + status() + ". Body: " + raw.body());
        }
    }
}
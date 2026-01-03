package dev.realmofevil.automation.engine.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthSession {

    private final String username;
    private final String password;
    private final boolean sharedFallback;

    private String token;
    private final Map<String, String> cookies = new ConcurrentHashMap<>();

    public AuthSession(String username, String password, boolean sharedFallback) {
        this.username = username;
        this.password = password;
        this.sharedFallback = sharedFallback;
    }

    public String username() { return username; }
    public String password() { return password; }

    public boolean isSharedFallback() {
        return sharedFallback;
    }

    public String token() { return token; }
    public void token(String token) { this.token = token; }

    public void addCookie(String name, String value) {
        cookies.put(name, value);
    }

    public Map<String, String> cookies() {
        return cookies;
    }
}

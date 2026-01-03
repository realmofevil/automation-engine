package dev.realmofevil.automation.engine.auth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe holder for authentication artifacts (Tokens, Cookies).
 * Stored inside ExecutionContext.
 */
public final class AuthSession {
    private final Map<String, String> cookies = new ConcurrentHashMap<>();
    private volatile String authToken;
    private volatile String username;
    private volatile String password;

    private volatile String transportUser;
    private volatile String transportPassword;

    public boolean isAuthenticated() {
        return authToken != null || !cookies.isEmpty();
    }

    public void setAuthToken(String token) {
        this.authToken = token;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void addCookie(String name, String value) {
        cookies.put(name, value);
    }

    public String getCookieHeader() {
        if (cookies.isEmpty())
            return null;
        StringBuilder sb = new StringBuilder();
        cookies.forEach((k, v) -> sb.append(k).append("=").append(v).append("; "));
        return sb.toString();
    }

    public void setCurrentUser(String username) {
        this.username = username;
    }

    public String getCurrentUser() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setTransportUser(String u) {
        this.transportUser = u;
    }

    public String getTransportUser() {
        return transportUser != null ? transportUser : username;
    }

    public void setTransportPassword(String p) {
        this.transportPassword = p;
    }

    public String getTransportPassword() {
        return transportPassword != null ? transportPassword : password;
    }

    /**
     * Clears authentication artifacts to force a fresh login.
     * Does NOT clear the credentials (username/password), only the session result (tokens/cookies).
     */
    public void invalidate() {
        this.authToken = null;
        this.cookies.clear();
    }
}

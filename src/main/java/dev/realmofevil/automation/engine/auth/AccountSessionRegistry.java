package dev.realmofevil.automation.engine.auth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class AccountSessionRegistry {

    private final ConcurrentMap<String, SessionState> sessions = new ConcurrentHashMap<>();

    public SessionState sessionFor(AccountCredentials account) {
        return sessions.computeIfAbsent(
                account.id(),
                id -> new SessionState());
    }
}
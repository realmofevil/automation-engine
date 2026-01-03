package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.config.AuthAccount;
import dev.realmofevil.automation.engine.security.Base64Secrets;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AccountPool {

    private final BlockingQueue<AuthSession> pool;
    private final AuthSession sharedFallback;

    public AccountPool(List<AuthAccount> accounts) {
        pool = new LinkedBlockingQueue<>();

        accounts.forEach(acc -> pool.add(new AuthSession(
                Base64Secrets.decode(acc.username()),
                Base64Secrets.decode(acc.password()),
                false)));

        sharedFallback = pool.peek() != null
                ? new AuthSession(
                        pool.peek().username(),
                        pool.peek().password(),
                        true)
                : null;
    }

    public AuthSession acquire(boolean dedicated) {
        if (dedicated) {
            AuthSession session = pool.poll();
            if (session == null) {
                throw new IllegalStateException(
                        "No dedicated accounts available");
            }
            return session;
        }

        AuthSession session = pool.poll();
        return session != null ? session : sharedFallback;
    }

    public void release(AuthSession session) {
        if (session == null || session.isSharedFallback())
            return;
        pool.offer(session);
    }
}

/**
 * public class AccountPool {
 * 
 * private final BlockingQueue<AuthSession> pool;
 * private final AuthSession sharedFallbackSession;
 * 
 * public AccountPool(List<AuthAccount> accounts) {
 * pool = new LinkedBlockingQueue<>();
 * 
 * accounts.forEach(acc -> pool.add(new AuthSession(
 * Base64Secrets.decode(acc.username()),
 * Base64Secrets.decode(acc.password()),
 * false)));
 * 
 * // Shared fallback (uses first account or empty)
 * if (!accounts.isEmpty()) {
 * AuthAccount first = accounts.get(0);
 * sharedFallbackSession = new AuthSession(
 * Base64Secrets.decode(first.username()),
 * Base64Secrets.decode(first.password()),
 * true);
 * } else {
 * sharedFallbackSession = null;
 * }
 * }
 * 
 * public AuthSession acquire() {
 * AuthSession session = pool.poll();
 * return session != null ? session : sharedFallbackSession;
 * }
 * 
 * public void release(AuthSession session) {
 * if (session == null)
 * return;
 * 
 * // Do NOT return shared fallback to pool
 * if (!session.isSharedFallback()) {
 * pool.offer(session);
 * }
 * }
 * }
 **/

/**
 * public class AccountPool {
 * 
 * private final BlockingQueue<AuthSession> pool;
 * 
 * public AccountPool(Iterable<AuthAccount> accounts) {
 * pool = new LinkedBlockingQueue<>();
 * 
 * accounts.forEach(acc -> pool.add(new AuthSession(
 * Base64Secrets.decode(acc.username()),
 * Base64Secrets.decode(acc.password()))));
 * }
 * 
 * public AuthSession borrow() {
 * try {
 * return pool.take();
 * } catch (InterruptedException e) {
 * throw new RuntimeException(e);
 * }
 * }
 * 
 * public void release(AuthSession session) {
 * pool.offer(session);
 * }
 * }
 **/
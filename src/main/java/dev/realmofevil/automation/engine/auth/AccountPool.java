package dev.realmofevil.automation.engine.auth;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class AccountPool {

    private final List<AccountCredentials> accounts;
    private final AtomicInteger cursor = new AtomicInteger(0);
    private final Set<String> inUse = ConcurrentHashMap.newKeySet();

    private final ThreadLocal<AccountCredentials> pinned = new ThreadLocal<>();

    public AccountPool(List<AccountCredentials> accounts) {
        this.accounts = List.copyOf(accounts);
    }

    public void pin(AccountCredentials account) {
        pinned.set(account);
    }

    public void clearPin() {
        pinned.remove();
    }

    public AccountCredentials current() {
        AccountCredentials pinnedAccount = pinned.get();
        if (pinnedAccount != null) {
            return pinnedAccount;
        }
        return next();
    }

    public AccountCredentials next() {
        for (int i = 0; i < accounts.size(); i++) {
            AccountCredentials acc = accounts.get(
                    Math.abs(cursor.getAndIncrement())
                            % accounts.size());

            if (inUse.add(acc.id())) {
                return acc;
            }
            /**
             * TestReporter.warn(
             * "Primary account unavailable, falling back to the next one: " + acc.id()
             * );
             * 
             **/
        }
        throw new IllegalStateException(
                "No free accounts available");
    }

    public void release(AccountCredentials account) {
        inUse.remove(account.id());
    }

    public AccountCredentials byId(String id) {
        return accounts.stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
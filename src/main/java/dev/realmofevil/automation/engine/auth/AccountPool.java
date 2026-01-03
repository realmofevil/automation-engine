package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.config.OperatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Manages the lifecycle and concurrency of User Accounts.
 * Prevents two threads from using the same credentials simultaneously.
 */
public final class AccountPool {

    private static final Logger LOG = LoggerFactory.getLogger(AccountPool.class);

    private final Map<String, OperatorConfig.ApiAccount> accountConfig;

    // Active Leases: Key = Account Alias, Value = Thread Name (for debugging)
    private final ConcurrentHashMap<String, String> activeLeases = new ConcurrentHashMap<>();

    // Fairness control: Global semaphore to limit concurrent users to total account count
    // This handles the "More Threads than Accounts" edge case by making threads wait here.
    private final Semaphore availabilitySemaphore;

    public AccountPool(Map<String, OperatorConfig.ApiAccount> accountConfig) {
        this.accountConfig = accountConfig;
        // Permit count = total number of accounts available
        this.availabilitySemaphore = new Semaphore(accountConfig.size(), true);
    }

    /**
     * Leases a specific account by alias (e.g., "vip").
     * Blocks if the account is currently in use.
     */
    public synchronized OperatorConfig.ApiAccount leaseSpecific(String alias) {
        if (!accountConfig.containsKey(alias)) {
            throw new IllegalArgumentException("Account alias not found: " + alias);
        }

        long start = System.currentTimeMillis();
        long timeout = 30000;

        while (activeLeases.containsKey(alias)) {
            if (System.currentTimeMillis() - start > timeout) {
                throw new RuntimeException("Timeout waiting for specific account: " + alias);
            }
            try {
                wait(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for account", e);
            }
        }

        activeLeases.put(alias, Thread.currentThread().getName());
        return accountConfig.get(alias);
    }

    /**
     * Leases ANY available account marked as `isPool: true`.
     * Blocks if all pool accounts are busy.
     */
    public OperatorConfig.ApiAccount leaseAny() {
        try {
            if (!availabilitySemaphore.tryAcquire(60, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout: No free accounts available in pool after 60s");
            }

            synchronized (this) {
                for (Map.Entry<String, OperatorConfig.ApiAccount> entry : accountConfig.entrySet()) {
                    String alias = entry.getKey();
                    OperatorConfig.ApiAccount acc = entry.getValue();

                    if (acc.isPool() && !activeLeases.containsKey(alias)) {
                        activeLeases.put(alias, Thread.currentThread().getName());
                        LOG.info("Leased pool account: {}", alias);
                        return acc;
                    }
                }
            }

            availabilitySemaphore.release();
            throw new RuntimeException("Semaphore acquired but no pool account found. Config error?");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for pool account", e);
        }
    }

    /**
     * Returns an account to the pool.
     */
    public synchronized void release(OperatorConfig.ApiAccount account) {
        String aliasToRelease = null;
        for (var entry : accountConfig.entrySet()) {
            if (entry.getValue().equals(account)) {
                aliasToRelease = entry.getKey();
                break;
            }
        }

        if (aliasToRelease != null && activeLeases.containsKey(aliasToRelease)) {
            activeLeases.remove(aliasToRelease);
            availabilitySemaphore.release();
            notifyAll();
            LOG.debug("Released account: {}", aliasToRelease);
        }
    }
}
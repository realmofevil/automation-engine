package dev.realmofevil.automation.engine.db;

import java.sql.Connection;

final class PooledConnection {

    private final Connection connection;
    private boolean inUse;

    PooledConnection(Connection connection) {
        this.connection = connection;
    }

    synchronized boolean tryAcquire() {
        if (inUse) {
            return false;
        }
        inUse = true;
        return true;
    }

    synchronized void release() {
        inUse = false;
    }

    Connection connection() {
        return connection;
    }
}
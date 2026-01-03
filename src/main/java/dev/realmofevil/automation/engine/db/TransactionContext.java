package dev.realmofevil.automation.engine.db;

import java.sql.Connection;

public final class TransactionContext {

    private final Connection connection;
    private boolean commitRequested;

    TransactionContext(Connection connection) {
        this.connection = connection;
    }

    public Connection connection() {
        return connection;
    }

    public void requestCommit() {
        this.commitRequested = true;
    }

    boolean shouldCommit() {
        return commitRequested;
    }
}
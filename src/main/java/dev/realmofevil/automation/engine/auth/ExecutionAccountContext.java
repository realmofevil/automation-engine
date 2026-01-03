package dev.realmofevil.automation.engine.auth;

public final class ExecutionAccountContext {

    private final AccountCredentials account;
    private final AccountSelectionMode mode;

    public ExecutionAccountContext(
            AccountCredentials account,
            AccountSelectionMode mode
    ) {
        this.account = account;
        this.mode = mode;
    }

    public AccountCredentials account() {
        return account;
    }

    public AccountSelectionMode mode() {
        return mode;
    }
}
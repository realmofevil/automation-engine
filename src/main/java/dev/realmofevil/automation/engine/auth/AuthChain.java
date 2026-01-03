package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.context.ExecutionContext;

import java.util.List;

public final class AuthChain {

    private final List<AuthStrategy> strategies;

    public AuthChain(List<AuthStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public AuthResult authenticate(ExecutionContext ctx) {
        for (AuthStrategy s : strategies) {
            AuthResult r = s.authenticate(ctx);
            if (r.success()) {
                return r;
            }
        }
        throw new IllegalStateException("Authentication failed for operator " + ctx.operator().name());
    }
}
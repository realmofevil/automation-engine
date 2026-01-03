package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.context.ExecutionContext;

public interface AuthStrategy {
    AuthResult authenticate(ExecutionContext ctx);
}
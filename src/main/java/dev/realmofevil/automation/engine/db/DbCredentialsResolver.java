package dev.realmofevil.automation.engine.db;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.security.CredentialDecoder;

public final class DbCredentialsResolver {

    private DbCredentialsResolver() {}

    public static DbCredentials resolve(ExecutionContext context) {
        return new DbCredentials(
                CredentialDecoder.decode(
                        context.operator().database().usernameB64()
                ),
                CredentialDecoder.decode(
                        context.operator().database().passwordB64()
                )
        );
    }
}
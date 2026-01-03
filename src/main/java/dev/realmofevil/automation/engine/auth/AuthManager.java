package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthManager {

    private static final Logger LOG = LoggerFactory.getLogger(AuthManager.class);
    private final ExecutionContext context;
    private final LoginClient loginClient;

    public AuthManager(ExecutionContext context) {
        this.context = context;
        this.loginClient = new LoginClient(context);
    }

    /**
     * Authenticates the context for the requested user alias.
     * Executes the chain of authentication definitions configured in YAML.
     */
    public void ensureAuthenticated(String primaryAccountAlias) {
        OperatorConfig config = context.config();
        
        OperatorConfig.ApiAccount primaryAccount = resolveAccount(primaryAccountAlias);

        context.auth().setCurrentUser(primaryAccount.username().plainText());
        context.auth().setPassword(primaryAccount.password().plainText());

        LOG.info("Initializing authentication for operator '{}' as user '{}'", config.id(), primaryAccountAlias);

        if (config.auth() != null) {
            for (OperatorConfig.AuthDefinition def : config.auth()) {
                OperatorConfig.ApiAccount stepAccount = primaryAccount;

                if (def.useAccount() != null && !def.useAccount().isBlank()) {
                    stepAccount = resolveAccount(def.useAccount());
                }

                applyAuthStrategy(def, stepAccount);
            }
        }
    }

    private OperatorConfig.ApiAccount resolveAccount(String alias) {
        OperatorConfig.ApiAccount account = context.config().accounts().get(alias);
        if (account == null) {
            throw new IllegalArgumentException(
                "Account alias '" + alias + "' not found in operator config '" + context.config().id() + "'"
            );
        }
        return account;
    }

    public void applyTransportAuthOnly() {
        OperatorConfig config = context.config();
        
        if (config.auth() != null) {
            for (OperatorConfig.AuthDefinition def : config.auth()) {
                // Only apply strategies that specify a 'useAccount' (Transport Layer)
                if (def.useAccount() != null) {
                    OperatorConfig.ApiAccount transportAcc = resolveAccount(def.useAccount());

                    context.auth().setTransportUser(transportAcc.username().plainText());
                    context.auth().setTransportPassword(transportAcc.password().plainText());
                }
            }
        }
    }

    /**
     * Called by ApiClient when a 401 is encountered.
     * Invalidates the current session and re-executes the auth strategy 
     * for the currently leased account.
     */
    public void reauthenticate() {
        OperatorConfig.ApiAccount currentAccount = context.getLeasedAccount();
        
        // If no account is leased, it might be a Public test or Transport-only test.
        if (currentAccount == null) {
            LOG.warn("Received 401 but no user account is leased. Cannot re-authenticate.");
            return;
        }

        LOG.info("Session expired for user '{}'. Refreshing authentication...", 
                 currentAccount.username().plainText());

        context.auth().invalidate();

        if (context.config().auth() != null) {
            for (OperatorConfig.AuthDefinition def : context.config().auth()) {
                applyAuthStrategy(def, currentAccount);
            }
        }
    }

    private void applyAuthStrategy(OperatorConfig.AuthDefinition def, OperatorConfig.ApiAccount account) {
        switch (def.type()) {
            case LOGIN_TOKEN:
                if (context.auth().getAuthToken() == null) {
                    loginClient.login(account, def);
                }
                break;
                
            case BASIC_HEADER:
            case BASIC_URL:
                // For Basic Auth, we might need to store these specific credentials 
                // if they differ from the primary user.
                // ApiClient reads user/pass from AuthSession.
                // If Basic Auth uses a different user (e.g. Proxy), we handle it by 
                // storing it in a dedicated "System Credentials" slot in AuthSession 
                // or updating ApiClient to handle multiple credential sets.
                
                // If useAccount is set, we assume these are the 
                // credentials needed for the transport layer (Basic Auth).
                if (def.useAccount() != null) {
                    context.auth().setTransportUser(account.username().plainText());
                    context.auth().setTransportPassword(account.password().plainText());
                }
                break;
                
            case SESSION_COOKIE:
                 break;
        }
    }

    /**
     * Called by Test Lifecycle @BeforeEach
     * @param requestedAlias specific alias (e.g. "vip") or null for "any".
     */
    public void acquireAccount(String requestedAlias) {
        OperatorConfig.ApiAccount account;

        if (requestedAlias != null) {
            account = context.getAccountPool().leaseSpecific(requestedAlias);
        } else {
            account = context.getAccountPool().leaseAny();
        }

        context.setLeasedAccount(account);

        authenticateContext(account);
    }

    public void releaseAccount() {
        OperatorConfig.ApiAccount current = context.getLeasedAccount();
        if (current != null) {
            context.getAccountPool().release(current);
            context.setLeasedAccount(null);
        }
    }
}
package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.config.OperatorConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.reporting.SmartRedactor;
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
     * Authenticates using a specific alias (e.g. "vip") without leasing from pool.
     */
    public void ensureAuthenticated(String primaryAccountAlias) {
        OperatorConfig.ApiAccount primaryAccount = resolveAccount(primaryAccountAlias);
        authenticateContext(primaryAccount);
    }

    /**
     * Logic used by both ensureAuthenticated (Specific) and acquireAccount (Pool).
     */
    private void authenticateContext(OperatorConfig.ApiAccount account) {
        OperatorConfig config = context.config();

        context.auth().setCurrentUser(account.username().plainText());
        context.auth().setPassword(account.password().plainText());

        String maskedUser = SmartRedactor.maskValue(account.username().plainText());
        LOG.info("Initializing authentication for operator '{}' as user '{}'", config.id(), maskedUser);

        if (config.auth() != null) {
            for (OperatorConfig.AuthDefinition def : config.auth()) {
                OperatorConfig.ApiAccount stepAccount = account;

                if (def.useAccount() != null && !def.useAccount().isBlank()) {
                    stepAccount = resolveAccount(def.useAccount());
                }

                applyAuthStrategy(def, stepAccount);
            }
        }
    }

    /**
     * Applies only transport-layer authentication (e.g. Basic Auth) without session/login.
     * Used for tests that do not require user-level auth.
     */
    public void applyTransportAuthOnly() {
        OperatorConfig config = context.config();

        if (config.auth() != null) {
            for (OperatorConfig.AuthDefinition def : config.auth()) {
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
     * 
     * @param requestedAlias specific alias (e.g. "vip") or null for "any".
     */
    public void acquireAccount(String requestedAlias) {
        OperatorConfig.ApiAccount account;

        if (requestedAlias != null && !requestedAlias.isBlank()) {
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
            // Optional: Logout here if "Clean Session" is required.
            // BUT TESTS SHOULD BE AGNOSTIC TO THE CONTEXT AND NOT CARE ABOUT AUTH STATE, THIS IS INFRASTRUCTURE, NOT TEST LOGIC.
            // IF LOGOUT FAILS, WE STILL WANT TO RELEASE THE ACCOUNT BACK TO THE POOL AND NOT BLOCK FUTURE TESTS.
            try {
                loginClient.logout(current);
            } catch (Exception e) {
            }

            context.auth().invalidate();

            context.getAccountPool().release(current);
            context.setLeasedAccount(null);
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
}
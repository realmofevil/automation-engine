package dev.realmofevil.automation.engine.context;

import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.config.ExecutionConfig;
import dev.realmofevil.automation.engine.config.OperatorEndpoint;

import java.util.Map;

/**
 * Execution context for the current test thread.
 *
 * Holds:
 *  - environment configuration
 *  - active operator
 *  - operator-specific endpoint
 *
 * Thread-safe via ThreadLocal.
 */
public final class ExecutionContext {

    private static EnvironmentConfig environmentConfig;

    private static final ThreadLocal<String> currentOperator =
            new ThreadLocal<>();

    private static final ThreadLocal<OperatorEndpoint> currentEndpoint =
            new ThreadLocal<>();

    private static final ThreadLocal<ExecutionConfig> CTX = new ThreadLocal<>();

    private ExecutionContext() {
        // no instances
    }

    /**
     * Initializes the global execution context.
     * Must be called once during bootstrap.
     */
    public static void init(EnvironmentConfig envConfig, String operator) {
        environmentConfig = envConfig;

        if (!"all".equalsIgnoreCase(operator)) {
            setOperator(operator);
        }
    }

    /**
     * Sets the operator for the current thread.
     */
    public static void setOperator(String operator) {
        Map<String, OperatorEndpoint> operators =
                environmentConfig.operators();

        OperatorEndpoint endpoint = operators.get(operator);

        if (endpoint == null) {
            throw new IllegalStateException(
                    "Operator not found in configuration: " + operator
            );
        }

        currentOperator.set(operator);
        currentEndpoint.set(endpoint);
    }

    /**
     * Stores the full execution configuration for the current thread.
     */
    public static void set(ExecutionConfig config) {
        CTX.set(config);
        // synchronize static environment for compatibility with bootstrap/init
        environmentConfig = config.environment();

        String operator = config.suite() == null ? null : config.suite().operator();
        if (operator != null && !"all".equalsIgnoreCase(operator)) {
            setOperator(operator);
        }
    }

    /**
     * Returns the current execution configuration.
     */
    public static ExecutionConfig get() {
        ExecutionConfig config = CTX.get();
        if (config == null) {
            throw new IllegalStateException("Execution context not set for current thread");
        }
        return config;
    }

    /**
     * Returns the environment configuration.
     */
    public static EnvironmentConfig environment() {
        return environmentConfig;
    }

    /**
     * Returns the current operator name.
     */
    public static String operator() {
        String operator = currentOperator.get();
        if (operator == null) {
            throw new IllegalStateException(
                    "Operator not set for current thread"
            );
        }
        return operator;
    }

    /**
     * Returns the operator endpoint for the current thread.
     */
    public static OperatorEndpoint endpoint() {
        OperatorEndpoint endpoint = currentEndpoint.get();
        if (endpoint == null) {
            throw new IllegalStateException(
                    "Operator endpoint not set for current thread"
            );
        }
        return endpoint;
    }

    /**
     * Clears thread-local context (important for parallel execution).
     */
    public static void clear() {
        currentOperator.remove();
        currentEndpoint.remove();
    }
}

/**
// Alternative simplified implementation
package dev.realmofevil.automation.engine.context;

import dev.realmofevil.automation.engine.config.ExecutionConfig;

public final class ExecutionContext {

    private static final ThreadLocal<ExecutionConfig> CTX = new ThreadLocal<>();

    private ExecutionContext() {
    }

    public static void set(ExecutionConfig config) {
        CTX.set(config);
    }

    public static ExecutionConfig get() {
        return CTX.get();
    }
}
**/
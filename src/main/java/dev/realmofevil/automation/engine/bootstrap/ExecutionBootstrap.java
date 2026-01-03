package dev.realmofevil.automation.engine.bootstrap;

import dev.realmofevil.automation.engine.config.ConfigLoader;
import dev.realmofevil.automation.engine.config.ConfigValidator;
import dev.realmofevil.automation.engine.config.EnvironmentConfig;
import dev.realmofevil.automation.engine.context.ExecutionContext;

import java.util.Optional;

/**
 * Entry-point bootstrap for the automation framework.
 *
 * Responsibilities:
 *  - Load environment configuration (YAML)
 *  - Apply CLI overrides
 *  - Validate configuration (fail-fast)
 *  - Initialize ExecutionContext
 *
 * This class must be invoked once before any test execution.
 */
public final class ExecutionBootstrap {

    private ExecutionBootstrap() {}

    /**
     * Bootstraps the execution context.
     *
     * Expected system properties:
     *  - server   (environment name, e.g. dev, qa, prod)
     *  - operator (operator name or "all")
     *
     * @return initialized EnvironmentConfig
     */
    public static EnvironmentConfig bootstrap() {

        String environment =
                Optional.ofNullable(System.getProperty("server"))
                        .orElse("dev");

        String operator =
                Optional.ofNullable(System.getProperty("operator"))
                        .orElse("all");

        EnvironmentConfig envConfig = ConfigLoader.load(environment);

        ConfigValidator.validate(envConfig);

        ExecutionContext.init(envConfig, operator);

        return envConfig;
    }
}

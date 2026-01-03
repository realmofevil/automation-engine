package dev.realmofevil.automation.engine.auth.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class or method as Public.
 * <p>
 * Effect:
 * 1. The test will NOT lease a user account from the pool (preventing starvation).
 * 2. The test will NOT perform a Session Login (Layer 2).
 * 3. The test WILL still apply Transport Layer Auth (Basic Auth) if configured in YAML.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Public {
}
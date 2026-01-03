package dev.realmofevil.automation.engine.junit;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Flaky {
    int retries() default 2;
}
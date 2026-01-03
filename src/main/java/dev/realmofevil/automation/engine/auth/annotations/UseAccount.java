package dev.realmofevil.automation.engine.auth.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface UseAccount {
    String id();
}
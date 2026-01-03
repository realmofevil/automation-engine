package dev.realmofevil.automation.engine.db.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface CommitTransaction {
}
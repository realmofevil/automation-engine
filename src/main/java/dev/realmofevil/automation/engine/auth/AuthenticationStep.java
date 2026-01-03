package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.ApiRequest;

public interface AuthenticationStep {

    ApiRequest apply(
            ExecutionContext context,
            ApiRequest request
    );
}
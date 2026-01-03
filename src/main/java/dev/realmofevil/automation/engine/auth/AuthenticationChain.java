package dev.realmofevil.automation.engine.auth;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.http.ApiRequest;

import java.util.List;

public final class AuthenticationChain {

    private final List<AuthenticationStep> steps;

    public AuthenticationChain(List<AuthenticationStep> steps) {
        this.steps = List.copyOf(steps);
    }

    /**
     * TestReporter.info(
     * "Executing auth chain: " + chain.name() //+ step.name() + " with params: " +
     * step.params() + " and auth context: " + authContext.toString()
     * );
     * 
     **/

    public ApiRequest apply(
            ExecutionContext context,
            ApiRequest request) {
        AuthContext authContext = new AuthContext();
        ApiRequest current = request;

        for (AuthenticationStep step : steps) {
            current = step.apply(context, current, authContext);
        }
        return current;
    }
}
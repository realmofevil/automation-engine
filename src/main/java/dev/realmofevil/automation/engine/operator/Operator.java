package dev.realmofevil.automation.engine.operator;

import dev.realmofevil.automation.engine.auth.AuthChain;
import dev.realmofevil.automation.engine.routing.RouteRegistry;

public final class Operator {

    private final String name;
    private final String domain;
    private final RouteRegistry routes;
    private final AuthChain authChain;
    private final int parallelism;

    public Operator(
            String name,
            String domain,
            RouteRegistry routes,
            AuthChain authChain,
            int parallelism
    ) {
        this.name = name;
        this.domain = domain;
        this.routes = routes;
        this.authChain = authChain;
        this.parallelism = parallelism;
    }

    public String name() {
        return name;
    }

    public String domain() {
        return domain;
    }

    public RouteRegistry routes() {
        return routes;
    }

    public AuthChain authChain() {
        return authChain;
    }

    public int parallelism() {
        return parallelism;
    }
}
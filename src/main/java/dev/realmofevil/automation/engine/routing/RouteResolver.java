package dev.realmofevil.automation.engine.routing;

import dev.realmofevil.automation.engine.operator.OperatorDomains;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public final class RouteResolver {

    private final OperatorDomains domains;
    private final Map<RouteKey, RouteDefinition> routes;

    public RouteResolver(
            OperatorDomains domains,
            Map<RouteKey, RouteDefinition> routes
    ) {
        this.domains = Objects.requireNonNull(domains);
        this.routes = Map.copyOf(routes);
    }

    public URI resolveDesktop(RouteKey key) {
        return domains.desktop().resolve(routes.get(key).path());
    }

    public URI resolveMobile(RouteKey key) {
        return domains.mobile().resolve(routes.get(key).path());
    }
}
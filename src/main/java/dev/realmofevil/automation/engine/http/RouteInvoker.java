package dev.realmofevil.automation.engine.http;

import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.routing.RouteDefinition;
import dev.realmofevil.automation.engine.reporting.TestReporter;

import java.net.URI;
import java.net.http.HttpRequest;

public final class RouteInvoker {

    private final ExecutionContext context;
    private final ApiClient client;

    public RouteInvoker(ExecutionContext context) {
        this.context = context;
        this.client = ApiClient.from(context);
    }

    public ApiResponse invokeDesktop(String routeKey) {
        return invoke(routeKey, context.operator().desktopDomain());
    }

    public ApiResponse invokeMobile(String routeKey) {
        return invoke(routeKey, context.operator().mobileDomain());
    }

    private ApiResponse invoke(String routeKey, URI base) {
        RouteDefinition def = context.routes().get(routeKey);

        URI uri = base.resolve(def.path());

        ApiRequest request = ApiRequest.builder()
                .uri(uri)
                .method(
                        def.method().name(),
                        HttpRequest.BodyPublishers.noBody())
                .build();

        /**
         * TestReporter.info(
         * "Invoking route: " + route.key() +
         * " [" + request.method() + " " + request.uri() + "]"
         * );
         **/

        ApiRequest authenticated = context.auth().apply(context, request);

        return client.send(authenticated);
    }
}
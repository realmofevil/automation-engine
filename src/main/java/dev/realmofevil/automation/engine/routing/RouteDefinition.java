package dev.realmofevil.automation.engine.routing;

import java.net.http.HttpRequest;

public record RouteDefinition(HttpRequest.Method method, String path) {
}
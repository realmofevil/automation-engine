package dev.realmofevil.automation.engine.routing;

import java.net.URI;
import java.net.http.HttpRequest;

public record Route(HttpRequest.Method method, URI uri) {
}
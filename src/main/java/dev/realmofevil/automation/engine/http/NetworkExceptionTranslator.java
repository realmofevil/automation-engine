package dev.realmofevil.automation.engine.http;

import dev.realmofevil.automation.engine.reporting.StepReporter;

import java.net.URI;

/**
 * Translates raw JVM networking exceptions into actionable, human-readable domain messages.
 */
public final class NetworkExceptionTranslator {

    private NetworkExceptionTranslator() {}

    public static RuntimeException translate(Exception e, URI targetUri, String context) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String host = targetUri != null ? targetUri.getHost() : "unknown host";

        String humanReadableMessage = switch (cause) {
            case java.nio.channels.UnresolvedAddressException _ -> 
                String.format("DNS Resolution Failed during %s: Cannot find IP address for '%s'. Check if the domain is correct, or if you need to be on a VPN.", context, host);
            
            case java.net.ConnectException _ -> 
                String.format("Connection Refused during %s: The server at '%s' actively refused the connection. The service might be down, or a firewall is blocking the port.", context, host);
            
            case java.net.http.HttpTimeoutException _ -> 
                String.format("Timeout during %s: The request to '%s' took longer than the configured timeout and was aborted.", context, host);
            
            case javax.net.ssl.SSLHandshakeException _ -> 
                String.format("SSL/TLS Handshake Failed during %s for '%s'. The certificate might be expired, invalid, or SNI mismatched. Try enabling relaxed SSL mode.", context, host);
            
            case java.net.SocketException se -> 
                String.format("Socket Error communicating with '%s' during %s: %s (E.g., Connection reset by peer).", host, context, se.getMessage());
            
            case java.net.UnknownHostException _ ->
                String.format("Unknown Host during %s: The machine cannot route to '%s'.", context, host);

            case java.io.IOException ioe -> 
                String.format("I/O Error during %s to '%s': %s", context, host, ioe.getMessage());
            
            default -> 
                String.format("Unexpected Execution Error during %s [%s]: %s", context, targetUri, cause.getMessage());
        };

        StepReporter.error("NETWORK FAILURE | " + humanReadableMessage, null);
        return new RuntimeException(humanReadableMessage, e);
    }
}
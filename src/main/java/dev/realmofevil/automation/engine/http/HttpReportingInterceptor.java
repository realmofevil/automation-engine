package dev.realmofevil.automation.engine.http;

import dev.realmofevil.automation.engine.reporting.TestReporter;

public final class HttpReportingInterceptor {

    public static void report(
            String routeKey,
            ApiResponse response
    ) {
        TestReporter.attach(
                "HTTP " + routeKey + " status",
                String.valueOf(response.statusCode())
        );
        TestReporter.attach(
                "HTTP " + routeKey + " body",
                response.body()
        );
    }
}

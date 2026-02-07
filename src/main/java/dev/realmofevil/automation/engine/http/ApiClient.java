package dev.realmofevil.automation.engine.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.realmofevil.automation.engine.context.ExecutionContext;
import dev.realmofevil.automation.engine.reporting.StepReporter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

public class ApiClient {
    private final ExecutionContext context;
    private final ObjectMapper mapper;
    private final HttpClient defaultClient;
    private final HttpClient noRedirectsClient;

    private static final String SSL_MODE_PROPERTY = "engine.ssl.mode";

    public ApiClient(ExecutionContext context) {
        this.context = context;
        this.mapper = new ObjectMapper();

        HttpClient.Builder baseBuilder = configureSecurityAndProxy(context);

        this.defaultClient = baseBuilder
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.noRedirectsClient = baseBuilder
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Configures SSL Relaxation (if non-prod) and Proxy settings (if set via System Properties).
     * Replaces RestAssured.useRelaxedHTTPSValidation() and RestAssured.proxy().
     * System Properties:
     *  - engine.ssl.mode = [strict|relaxed]
     *  - proxy.server = [hostname]
     *  - proxy.port = [port number]
     */
    private HttpClient.Builder configureSecurityAndProxy(ExecutionContext ctx) {
        HttpClient.Builder builder = HttpClient.newBuilder();

        boolean isProd = "prod".equalsIgnoreCase(ctx.config().environment());

        String defaultMode = isProd ? "strict" : "relaxed";

        String sslMode = System.getProperty(SSL_MODE_PROPERTY, defaultMode);
        if ("relaxed".equalsIgnoreCase(sslMode)) {
            builder.sslContext(createInsecureSslContext());
            StepReporter.warn("SSL Verification DISABLED (" + sslMode + " mode)");
        } else {
            StepReporter.info("SSL Verification ENABLED (" + sslMode + " mode)");
        }

        String proxyHost = System.getProperty("proxy.server");
        String proxyPort = System.getProperty("proxy.port");
        if (proxyHost != null && proxyPort != null) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))));
            StepReporter.info("API Client routed through proxy: " + proxyHost + ":" + proxyPort);
        }

        return builder;
    }

    private SSLContext createInsecureSslContext() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure relaxed SSL context", e);
        }
    }

    /**
     * Starts a request based on a Route Key defined in YAML configuration.
     * Preferred for standard API testing.
     */
    public ApiRequestSpec route(String routeKey) {
        String ua = String.valueOf(context.config().contextDefaults().get("userAgent"));
        return new ApiRequestSpec(context, routeKey, this).header("User-Agent", ua);
    }

    /**
     * Starts a request based on a Raw/Absolute URL.
     * Use this when following redirects to external domains (e.g. Game Vendors).
     */
    public ApiRequestSpec url(String rawUrl) {
        String ua = String.valueOf(context.config().contextDefaults().get("userAgent"));
        return new ApiRequestSpec(context, URI.create(rawUrl), this).header("User-Agent", ua);
    }

    public ValidatableResponse post(String routeKey, Object body) {
        return route(routeKey).post(body);
    }

    public ValidatableResponse get(String routeKey) {
        return route(routeKey).get();
    }

    public HttpClient getNativeClient(boolean followRedirects) {
        return followRedirects ? defaultClient : noRedirectsClient;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}
package com.ptagent.web;

import com.ptagent.common.AppConfig;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.net.URI;

public final class SecurityHeaders {
    private SecurityHeaders() {
    }

    public static void apply(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "no-referrer");
        headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=(self)");
        headers.set("Cross-Origin-Opener-Policy", "same-origin");
        headers.set("Cross-Origin-Resource-Policy", "same-origin");
        headers.set("Content-Security-Policy",
                "default-src 'self'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'; "
                        + "script-src 'self' https://webapi.amap.com https://*.amap.com; "
                        + "style-src 'self' 'unsafe-inline' https://*.amap.com; "
                        + "img-src 'self' data: blob: https://*.amap.com; "
                        + "connect-src 'self' https://*.amap.com");
    }

    public static boolean applyCors(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin == null || origin.isBlank()) {
            return true;
        }
        if (!sameHost(origin, exchange.getRequestHeaders().getFirst("Host"))
                && !origin.equals(AppConfig.allowedOrigin())) {
            return false;
        }
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", origin);
        headers.set("Vary", "Origin");
        headers.set("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Request-Id");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PATCH, OPTIONS");
        headers.set("Access-Control-Max-Age", "600");
        return true;
    }

    private static boolean sameHost(String origin, String host) {
        if (host == null || host.isBlank()) {
            return false;
        }
        try {
            return host.equalsIgnoreCase(URI.create(origin).getAuthority());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}

package com.ptagent.common;

public final class AppConfig {
    private static final int DEFAULT_MAX_REQUEST_BYTES = 1024 * 1024;

    private AppConfig() {
    }

    public static String environment() {
        return text("PTAGENT_ENV", "local").toLowerCase();
    }

    public static boolean demoAuthEnabled() {
        return bool("PTAGENT_DEMO_AUTH", true);
    }

    public static String allowedOrigin() {
        return text("PTAGENT_ALLOWED_ORIGIN", "");
    }

    public static void validateStartup() {
        if ("production".equals(environment()) && demoAuthEnabled()) {
            throw new IllegalStateException("production环境禁止启用PTAGENT_DEMO_AUTH");
        }
        if (allowedOrigin().contains("*")) {
            throw new IllegalStateException("PTAGENT_ALLOWED_ORIGIN必须是精确来源，不能包含通配符");
        }
    }

    public static int maxRequestBytes() {
        String configured = text("PTAGENT_MAX_REQUEST_BYTES", "");
        if (configured.isBlank()) {
            return DEFAULT_MAX_REQUEST_BYTES;
        }
        try {
            int value = Integer.parseInt(configured);
            return Math.max(16 * 1024, Math.min(value, 10 * 1024 * 1024));
        } catch (NumberFormatException ignored) {
            return DEFAULT_MAX_REQUEST_BYTES;
        }
    }

    private static boolean bool(String name, boolean fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private static String text(String name, String fallback) {
        String value = System.getenv(name);
        return value == null ? fallback : value.trim();
    }
}

package com.ptagent.common;

public final class RequestContext {
    private static final ThreadLocal<Metadata> CURRENT = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(String requestId, String clientIp, String userAgent) {
        CURRENT.set(new Metadata(
                safe(requestId, 64),
                safe(clientIp, 64),
                safe(userAgent, 256)
        ));
    }

    public static Metadata current() {
        Metadata metadata = CURRENT.get();
        return metadata == null ? Metadata.EMPTY : metadata;
    }

    public static void clear() {
        CURRENT.remove();
    }

    private static String safe(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < value.length() && result.length() < maxLength; index++) {
            char character = value.charAt(index);
            if (!Character.isISOControl(character)) {
                result.append(character);
            }
        }
        return result.toString().trim();
    }

    public record Metadata(String requestId, String clientIp, String userAgent) {
        private static final Metadata EMPTY = new Metadata("", "", "");
    }
}

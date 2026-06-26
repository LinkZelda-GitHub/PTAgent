package com.ptagent.web;

import com.ptagent.common.Json;
import com.ptagent.exception.ErrorCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class Response {
    private static final String STATUS_ATTRIBUTE = "responseStatus";

    private Response() {
    }

    public static void json(HttpExchange exchange, int status, Object data) throws IOException {
        exchange.setAttribute(STATUS_ATTRIBUTE, status);
        byte[] bytes = Json.stringify(Json.object("ok", status < 400, "data", data)).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public static void error(HttpExchange exchange, int status, String message) throws IOException {
        error(exchange, status, ErrorCode.VALIDATION_ERROR, message);
    }

    public static void error(HttpExchange exchange, int status, ErrorCode code, String message) throws IOException {
        exchange.setAttribute(STATUS_ATTRIBUTE, status);
        String traceId = exchange.getResponseHeaders().getFirst("X-Request-Id");
        byte[] bytes = Json.stringify(Json.object("ok", false, "code", code.name(), "message", message,
                        "traceId", traceId == null ? "" : traceId))
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public static void noContent(HttpExchange exchange) throws IOException {
        exchange.setAttribute(STATUS_ATTRIBUTE, 204);
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }
}

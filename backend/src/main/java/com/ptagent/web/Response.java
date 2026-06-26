package com.ptagent.web;

import com.ptagent.common.Json;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class Response {
    private Response() {
    }

    public static void json(HttpExchange exchange, int status, Object data) throws IOException {
        byte[] bytes = Json.stringify(Json.object("ok", status < 400, "data", data)).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public static void error(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = Json.stringify(Json.object("ok", false, "message", message)).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    public static void noContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
        exchange.close();
    }
}

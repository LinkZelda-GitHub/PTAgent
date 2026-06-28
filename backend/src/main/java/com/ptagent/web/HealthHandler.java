package com.ptagent.web;

import com.ptagent.common.AppConfig;
import com.ptagent.common.AppVersion;
import com.ptagent.repository.Repository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class HealthHandler implements HttpHandler {
    private final Repository repository;

    public HealthHandler(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SecurityHeaders.apply(exchange);
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            Response.error(exchange, 405, com.ptagent.exception.ErrorCode.METHOD_NOT_ALLOWED, "仅支持GET");
            return;
        }
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("version", AppVersion.CURRENT);
        health.put("environment", AppConfig.environment());
        health.put("demoAuth", AppConfig.demoAuthEnabled());
        health.put("time", LocalDateTime.now().toString());
        health.put("repository", repository.storageType());
        if (!"production".equals(AppConfig.environment())) {
            health.put("database", repository.storageLocation());
            health.put("users", repository.allUsers().size());
            health.put("demands", repository.allDemands().size());
            health.put("orders", repository.allOrders().size());
        }
        Response.json(exchange, 200, health);
    }
}

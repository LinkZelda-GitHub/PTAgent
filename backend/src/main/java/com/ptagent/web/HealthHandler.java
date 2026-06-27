package com.ptagent.web;

import com.ptagent.common.Json;
import com.ptagent.repository.Repository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;

public class HealthHandler implements HttpHandler {
    private final Repository repository;

    public HealthHandler(Repository repository) {
        this.repository = repository;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            Response.error(exchange, 405, com.ptagent.exception.ErrorCode.METHOD_NOT_ALLOWED, "仅支持GET");
            return;
        }
        Response.json(exchange, 200, Json.object(
                "status", "UP",
                "time", LocalDateTime.now().toString(),
                "repository", repository.storageType(),
                "database", repository.storageLocation(),
                "users", repository.allUsers().size(),
                "demands", repository.allDemands().size(),
                "orders", repository.allOrders().size()
        ));
    }
}

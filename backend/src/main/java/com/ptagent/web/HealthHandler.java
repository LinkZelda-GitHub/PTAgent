package com.ptagent.web;

import com.ptagent.common.AppConfig;
import com.ptagent.common.AppVersion;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.Repository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class HealthHandler implements HttpHandler {
    public enum Mode {
        HEALTH, LIVE, READY, DEPENDENCIES
    }

    private final Repository repository;
    private final Mode mode;

    public HealthHandler(Repository repository) {
        this(repository, Mode.HEALTH);
    }

    public HealthHandler(Repository repository, Mode mode) {
        this.repository = repository;
        this.mode = mode;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        SecurityHeaders.apply(exchange);
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            Response.error(exchange, 405, ErrorCode.METHOD_NOT_ALLOWED, "仅支持GET");
            return;
        }
        Map<String, Object> response = switch (mode) {
            case LIVE -> liveness();
            case READY -> readiness();
            case DEPENDENCIES -> dependencies();
            case HEALTH -> health();
        };
        Response.json(exchange, "DOWN".equals(response.get("status")) ? 503 : 200, response);
    }

    private Map<String, Object> liveness() {
        return base("UP");
    }

    private Map<String, Object> readiness() {
        Map<String, Object> repositoryHealth = repository.health();
        boolean repositoryReady = "UP".equals(repositoryHealth.get("status"));
        boolean integrationReady = !"production".equals(AppConfig.environment());
        Map<String, Object> response = base(repositoryReady && integrationReady ? "UP" : "DOWN");
        response.put("repository", repositoryHealth);
        if (!integrationReady) {
            response.put("reason", "正式认证、数据库和分布式状态适配器尚未接入当前候选版");
        }
        return response;
    }

    private Map<String, Object> dependencies() {
        boolean production = "production".equals(AppConfig.environment());
        Map<String, Object> response = base(production ? "DOWN" : "DEGRADED");
        response.put("repository", repository.health());
        response.put("wechat", integration("WECHAT_APP_ID", "WECHAT_APP_SECRET"));
        response.put("qq", integration("QQ_APP_ID", "QQ_APP_KEY"));
        response.put("sms", integration("SMS_ACCESS_KEY_ID", "SMS_ACCESS_KEY_SECRET"));
        response.put("mysql", integration("MYSQL_URL", "MYSQL_USERNAME", "MYSQL_PASSWORD"));
        response.put("redis", integration("REDIS_URL"));
        response.put("objectStorage", integration("OSS_ENDPOINT", "OSS_BUCKET", "OSS_ACCESS_KEY_ID",
                "OSS_ACCESS_KEY_SECRET"));
        response.put("amap", integration("AMAP_WEB_KEY", "AMAP_SECURITY_CODE"));
        response.put("note", "CONFIGURED表示配置存在但当前候选版未连接，不能视为依赖可用");
        return response;
    }

    private Map<String, Object> health() {
        Map<String, Object> ready = readiness();
        Map<String, Object> response = base(String.valueOf(ready.get("status")));
        response.put("demoAuth", AppConfig.demoAuthEnabled());
        response.put("repository", repository.storageType());
        response.put("checks", Map.of("liveness", "UP", "readiness", ready.get("status")));
        if (!"production".equals(AppConfig.environment())) {
            response.put("database", repository.storageLocation());
            response.put("users", repository.allUsers().size());
            response.put("demands", repository.allDemands().size());
            response.put("orders", repository.allOrders().size());
        }
        return response;
    }

    private Map<String, Object> base(String status) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status);
        response.put("version", AppVersion.CURRENT);
        response.put("environment", AppConfig.environment());
        response.put("time", LocalDateTime.now().toString());
        return response;
    }

    private Map<String, Object> integration(String... variables) {
        boolean configured = true;
        for (String variable : variables) {
            String value = System.getenv(variable);
            configured &= value != null && !value.isBlank();
        }
        return Map.of(
                "status", configured ? "CONFIGURED" : "NOT_CONFIGURED",
                "connected", false
        );
    }
}

package com.ptagent.web;

import com.ptagent.common.Json;
import com.ptagent.common.AppConfig;
import com.ptagent.common.AppVersion;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.domain.User;
import com.ptagent.repository.Repository;
import com.ptagent.service.ApplicationService;
import com.ptagent.service.AuditService;
import com.ptagent.service.AuthService;
import com.ptagent.service.CourseService;
import com.ptagent.service.DashboardService;
import com.ptagent.service.DemandService;
import com.ptagent.service.FeedbackService;
import com.ptagent.service.DemandImportService;
import com.ptagent.service.NotificationService;
import com.ptagent.service.TeacherService;
import com.ptagent.web.controller.ApplicationController;
import com.ptagent.web.controller.AuditController;
import com.ptagent.web.controller.AuthController;
import com.ptagent.web.controller.CourseController;
import com.ptagent.web.controller.DashboardController;
import com.ptagent.web.controller.DemandController;
import com.ptagent.web.controller.FeedbackController;
import com.ptagent.web.controller.ImportController;
import com.ptagent.web.controller.NotificationController;
import com.ptagent.web.controller.TeacherController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ApiRouter implements HttpHandler {
    private static final String TRACE_ATTRIBUTE = "traceId";
    private static final String STATUS_ATTRIBUTE = "responseStatus";
    private static final String ACTOR_ATTRIBUTE = "actorId";
    private final List<ApiController> controllers;
    private final AuthService authService;

    public ApiRouter(Repository repository) {
        this.authService = new AuthService(repository);
        TeacherService teacherService = new TeacherService(repository);
        DemandService demandService = new DemandService(repository);
        ApplicationService applicationService = new ApplicationService(repository);
        CourseService courseService = new CourseService(repository);
        FeedbackService feedbackService = new FeedbackService(repository);
        DashboardService dashboardService = new DashboardService(repository);
        NotificationService notificationService = new NotificationService(repository);
        DemandImportService demandImportService = new DemandImportService(repository);
        AuditService auditService = new AuditService(repository);
        this.controllers = List.of(
                new DashboardController(dashboardService),
                new AuthController(this.authService),
                new DemandController(demandService),
                new ImportController(demandImportService),
                new ApplicationController(applicationService),
                new TeacherController(teacherService),
                new CourseController(courseService),
                new FeedbackController(feedbackService),
                new NotificationController(notificationService),
                new AuditController(auditService)
        );
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long start = System.nanoTime();
        String traceId = traceId(exchange);
        exchange.setAttribute(TRACE_ATTRIBUTE, traceId);
        exchange.getResponseHeaders().set("X-Request-Id", traceId);
        SecurityHeaders.apply(exchange);

        try {
            if (!SecurityHeaders.applyCors(exchange)) {
                Response.error(exchange, 403, ErrorCode.ORIGIN_NOT_ALLOWED, "请求来源不在允许列表");
                return;
            }
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                Response.noContent(exchange);
                return;
            }
            route(exchange);
        } catch (ApiException e) {
            Response.error(exchange, e.status(), e.code(), e.getMessage());
        } catch (IllegalArgumentException e) {
            Response.error(exchange, 400, ErrorCode.VALIDATION_ERROR, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Response.error(exchange, 500, ErrorCode.INTERNAL_ERROR, "服务异常：" + e.getMessage());
        } finally {
            log(exchange, start, null);
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        List<String> path = pathSegments(exchange);
        Map<String, String> query = query(exchange);
        Map<String, Object> body = body(exchange);
        ApiRequest request = new ApiRequest(method, path, query, body, bearerToken(exchange), null);

        if (path.isEmpty() && request.method("GET")) {
            Response.json(exchange, 200, Json.object(
                    "service", "PTAgent API",
                    "status", "running",
                    "version", AppVersion.CURRENT,
                    "environment", AppConfig.environment(),
                    "demoAuth", AppConfig.demoAuthEnabled()
            ));
            return;
        }
        if (!isPublic(request)) {
            User actor = authService.requireSession(request.bearerToken());
            exchange.setAttribute(ACTOR_ATTRIBUTE, actor.id);
            request = request.authenticated(actor);
        }
        for (ApiController controller : controllers) {
            if (controller.handle(request, exchange)) {
                return;
            }
        }

        Response.error(exchange, 404, ErrorCode.ROUTE_NOT_FOUND, "接口不存在");
    }

    private List<String> pathSegments(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        if (path.startsWith("/api")) {
            path = path.substring(4);
        }
        String[] raw = path.split("/");
        List<String> segments = new ArrayList<>();
        for (String segment : raw) {
            if (!segment.isBlank()) {
                segments.add(decode(segment));
            }
        }
        return segments;
    }

    private Map<String, String> query(HttpExchange exchange) {
        Map<String, String> query = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isBlank()) {
            return query;
        }
        for (String pair : raw.split("&")) {
            int split = pair.indexOf('=');
            if (split >= 0) {
                query.put(decode(pair.substring(0, split)), decode(pair.substring(split + 1)));
            } else {
                query.put(decode(pair), "");
            }
        }
        return query;
    }

    private Map<String, Object> body(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            return new LinkedHashMap<>();
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            throw new ApiException(415, ErrorCode.UNSUPPORTED_MEDIA_TYPE, "请求体必须使用application/json");
        }
        int limit = AppConfig.maxRequestBytes();
        byte[] bytes = exchange.getRequestBody().readNBytes(limit + 1);
        if (bytes.length > limit) {
            throw new ApiException(413, ErrorCode.REQUEST_TOO_LARGE, "请求体超过大小限制");
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        return Json.parseObject(text);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String traceId(HttpExchange exchange) {
        String requestId = exchange.getRequestHeaders().getFirst("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }

    private String bearerToken(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return "";
        }
        return authorization.substring(7).trim();
    }

    private boolean isPublic(ApiRequest request) {
        return request.is("bootstrap")
                || request.is("auth", "login")
                || request.is("auth", "phone-code")
                || request.is("auth", "register-teacher");
    }

    private void log(HttpExchange exchange, long start, String message) {
        long elapsedMs = Math.max(0, (System.nanoTime() - start) / 1_000_000);
        Object status = exchange.getAttribute(STATUS_ATTRIBUTE);
        System.out.println(Json.stringify(Json.object(
                "traceId", exchange.getAttribute(TRACE_ATTRIBUTE),
                "method", exchange.getRequestMethod(),
                "path", exchange.getRequestURI().getPath(),
                "actorId", exchange.getAttribute(ACTOR_ATTRIBUTE),
                "status", status == null ? 0 : status,
                "elapsedMs", elapsedMs,
                "message", message == null ? "" : message
        )));
    }
}

package com.ptagent.web;

import com.ptagent.common.Json;
import com.ptagent.repository.Repository;
import com.ptagent.service.ApplicationService;
import com.ptagent.service.AuthService;
import com.ptagent.service.CourseService;
import com.ptagent.service.DashboardService;
import com.ptagent.service.DemandService;
import com.ptagent.service.FeedbackService;
import com.ptagent.service.NotificationService;
import com.ptagent.service.TeacherService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiRouter implements HttpHandler {
    private final AuthService authService;
    private final TeacherService teacherService;
    private final DemandService demandService;
    private final ApplicationService applicationService;
    private final CourseService courseService;
    private final FeedbackService feedbackService;
    private final DashboardService dashboardService;
    private final NotificationService notificationService;

    public ApiRouter(Repository repository) {
        this.authService = new AuthService(repository);
        this.teacherService = new TeacherService(repository);
        this.demandService = new DemandService(repository);
        this.applicationService = new ApplicationService(repository);
        this.courseService = new CourseService(repository);
        this.feedbackService = new FeedbackService(repository);
        this.dashboardService = new DashboardService(repository);
        this.notificationService = new NotificationService(repository);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET,POST,PATCH,OPTIONS");
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            Response.noContent(exchange);
            return;
        }

        try {
            route(exchange);
        } catch (IllegalArgumentException e) {
            Response.error(exchange, 400, e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            Response.error(exchange, 500, "服务异常：" + e.getMessage());
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        List<String> path = pathSegments(exchange);
        Map<String, String> query = query(exchange);
        Map<String, Object> body = body(exchange);

        if (path.isEmpty() && "GET".equals(method)) {
            Response.json(exchange, 200, Json.object("service", "PTAgent API", "status", "running"));
            return;
        }
        if (is(path, "bootstrap") && "GET".equals(method)) {
            Response.json(exchange, 200, dashboardService.dashboard());
            return;
        }
        if (is(path, "auth", "login") && "POST".equals(method)) {
            Response.json(exchange, 200, authService.login(Json.str(body, "username"), Json.str(body, "password")));
            return;
        }
        if (is(path, "auth", "register-teacher") && "POST".equals(method)) {
            Response.json(exchange, 201, authService.registerTeacher(body));
            return;
        }
        if (is(path, "demands") && "GET".equals(method)) {
            Response.json(exchange, 200, demandService.listDemands(query));
            return;
        }
        if (is(path, "demands") && "POST".equals(method)) {
            Response.json(exchange, 201, demandService.createDemand(body));
            return;
        }
        if (path.size() == 2 && "demands".equals(path.get(0)) && "GET".equals(method)) {
            Response.json(exchange, 200, demandService.getDemand(parseId(path.get(1)), parseLong(query.get("teacherId"), 0)));
            return;
        }
        if (path.size() == 3 && "demands".equals(path.get(0)) && "close".equals(path.get(2)) && !"GET".equals(method)) {
            Response.json(exchange, 200, demandService.closeDemand(parseId(path.get(1))));
            return;
        }
        if (path.size() == 3 && "demands".equals(path.get(0)) && "applications".equals(path.get(2)) && "POST".equals(method)) {
            Response.json(exchange, 201, applicationService.apply(parseId(path.get(1)), body));
            return;
        }
        if (is(path, "applications") && "GET".equals(method)) {
            Response.json(exchange, 200, applicationService.listApplications(query.getOrDefault("status", "ALL")));
            return;
        }
        if (path.size() == 3 && "applications".equals(path.get(0)) && "review".equals(path.get(2)) && "POST".equals(method)) {
            Response.json(exchange, 200, applicationService.review(parseId(path.get(1)), body));
            return;
        }
        if (is(path, "teachers") && "GET".equals(method)) {
            Response.json(exchange, 200, teacherService.listTeachers());
            return;
        }
        if (path.size() == 3 && "teachers".equals(path.get(0)) && "enabled".equals(path.get(2)) && !"GET".equals(method)) {
            Response.json(exchange, 200, teacherService.setEnabled(parseId(path.get(1)), Json.bool(body, "enabled", true)));
            return;
        }
        if (path.size() == 3 && "teachers".equals(path.get(0)) && "profile".equals(path.get(2)) && !"GET".equals(method)) {
            Response.json(exchange, 200, teacherService.updateProfile(parseId(path.get(1)), body));
            return;
        }
        if (is(path, "resumes") && "GET".equals(method)) {
            Response.json(exchange, 200, teacherService.listResumes());
            return;
        }
        if (is(path, "resumes") && "POST".equals(method)) {
            Response.json(exchange, 201, teacherService.submitResume(body));
            return;
        }
        if (path.size() == 3 && "resumes".equals(path.get(0)) && "status".equals(path.get(2)) && !"GET".equals(method)) {
            Response.json(exchange, 200, teacherService.updateResumeStatus(parseId(path.get(1)), (int) Json.longValue(body, "status", 1)));
            return;
        }
        if (is(path, "orders") && "GET".equals(method)) {
            Response.json(exchange, 200, courseService.listOrders(parseLong(query.get("teacherId"), 0)));
            return;
        }
        if (path.size() == 3 && "orders".equals(path.get(0)) && "records".equals(path.get(2)) && "POST".equals(method)) {
            Response.json(exchange, 201, courseService.createRecord(parseId(path.get(1)), body));
            return;
        }
        if (is(path, "records") && "GET".equals(method)) {
            Response.json(exchange, 200, courseService.listRecords(parseLong(query.get("orderId"), 0)));
            return;
        }
        if (is(path, "feedbacks") && "GET".equals(method)) {
            Response.json(exchange, 200, feedbackService.listFeedbacks());
            return;
        }
        if (is(path, "feedbacks") && "POST".equals(method)) {
            Response.json(exchange, 201, feedbackService.createFeedback(body));
            return;
        }
        if (is(path, "notifications") && "GET".equals(method)) {
            Response.json(exchange, 200, notificationService.list(parseLong(query.get("userId"), 0)));
            return;
        }

        Response.error(exchange, 404, "接口不存在");
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
        String text = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return Json.parseObject(text);
    }

    private boolean is(List<String> path, String... expected) {
        if (path.size() != expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equals(path.get(i))) {
                return false;
            }
        }
        return true;
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private long parseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID格式不正确");
        }
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

package com.ptagent;

import com.ptagent.domain.ApplicationStatus;
import com.ptagent.domain.DemandStatus;
import com.ptagent.domain.User;
import com.ptagent.common.Json;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.AppRepository;
import com.ptagent.repository.Repository;
import com.ptagent.service.ApplicationService;
import com.ptagent.service.AuditService;
import com.ptagent.service.AuthService;
import com.ptagent.service.CourseService;
import com.ptagent.service.DemandImportService;
import com.ptagent.service.DemandService;
import com.ptagent.web.ApiRequest;
import com.ptagent.web.ApiRouter;
import com.ptagent.web.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceTests {
    public static void main(String[] args) {
        run("AuthService rejects invalid phone verification codes", ServiceTests::authInvalidPhoneCode);
        run("AuthService supports WeChat, QQ and phone sessions", ServiceTests::authSessionLifecycle);
        run("AuthService rate limits phone verification abuse", ServiceTests::authRateLimitsPhoneAbuse);
        run("ApiRequest overrides spoofed actor identifiers", ServiceTests::apiRequestUsesAuthenticatedActor);
        run("HTTP boundary enforces security policy", ServiceTests::httpSecurityBoundary);
        run("Teacher registration persists account and profile", ServiceTests::teacherRegistrationPersists);
        run("DemandService filters by subject and calculates match score", ServiceTests::demandFiltersAndScores);
        run("DemandService rejects invalid demand payloads", ServiceTests::demandValidation);
        run("DemandImportService imports xlsx order format", ServiceTests::demandXlsxImport);
        run("Access guard rejects teacher admin actions", ServiceTests::accessGuardRejectsTeacherAdminActions);
        run("ApplicationService blocks duplicated applications", ServiceTests::applicationDuplicateGuard);
        run("ApplicationService approval creates order and updates demand", ServiceTests::applicationApprovalCreatesOrder);
        run("AuditService lists critical operation logs", ServiceTests::auditLogsCriticalOperations);
        run("CourseService rejects records from wrong teacher", ServiceTests::courseRecordRequiresOrderTeacher);
        System.out.println("All service tests passed.");
    }

    private static void authInvalidPhoneCode() {
        Repository repository = new AppRepository(false);
        AuthService authService = new AuthService(repository);

        expectApiException(ErrorCode.AUTH_VERIFICATION_CODE_INVALID, "先获取手机验证码",
                () -> authService.login(body(
                        "loginMethod", "PHONE",
                        "loginId", "13800000003",
                        "verificationCode", "000000"
                )));
    }

    private static void authSessionLifecycle() {
        Repository repository = new AppRepository(false);
        AuthService authService = new AuthService(repository);

        Map<String, Object> wechatLogin = authService.login(body(
                "loginMethod", "WECHAT", "loginId", "ptagent_super"
        ));
        String token = String.valueOf(wechatLogin.get("token"));
        assertTrue(!token.isBlank(), "登录应签发会话令牌");

        @SuppressWarnings("unchecked")
        Map<String, Object> currentUser = (Map<String, Object>) authService.currentSession(token).get("user");
        assertEquals("WECHAT", currentUser.get("loginMethod"));

        Map<String, Object> qqLogin = authService.login(body(
                "loginMethod", "QQ", "loginId", "10001001"
        ));
        assertTrue(!String.valueOf(qqLogin.get("token")).isBlank(), "QQ登录应签发会话令牌");

        String phoneCode = String.valueOf(authService.issuePhoneCode("13800000003").get("demoCode"));
        Map<String, Object> phoneLogin = authService.login(body(
                "loginMethod", "PHONE",
                "loginId", "13800000003",
                "verificationCode", phoneCode
        ));
        assertTrue(!String.valueOf(phoneLogin.get("token")).isBlank(), "手机号登录应签发会话令牌");
        expectApiException(ErrorCode.AUTH_VERIFICATION_CODE_INVALID, "先获取手机验证码",
                () -> authService.login(body(
                        "loginMethod", "PHONE",
                        "loginId", "13800000003",
                        "verificationCode", phoneCode
                )));

        authService.logout(token);
        expectApiException(ErrorCode.AUTH_SESSION_INVALID, "登录状态已失效",
                () -> authService.currentSession(token));
    }

    private static void teacherRegistrationPersists() {
        Path directory = null;
        try {
            directory = Files.createTempDirectory("ptagent-account-test");
            Repository repository = new AppRepository(directory);
            AuthService authService = new AuthService(repository);
            String verificationCode = String.valueOf(authService.issuePhoneCode("13912345678").get("demoCode"));
            Map<String, Object> registered = authService.registerTeacher(body(
                    "loginMethod", "PHONE",
                    "loginId", "13912345678",
                    "verificationCode", verificationCode,
                    "realName", "测试教师",
                    "gender", 2,
                    "phoneNumber", "13912345678",
                    "email", "new_teacher@example.com",
                    "education", "本科",
                    "graduateSchool", "测试大学",
                    "subjects", List.of("数学", "物理"),
                    "serviceArea", List.of("天河区"),
                    "hasTeacherCert", true,
                    "personalIntro", "擅长理科基础巩固。"
            ));

            assertEquals("PENDING_REVIEW", registered.get("status"));
            long teacherId = number(castMap(registered.get("user")).get("id"));
            assertTrue(!repository.findUser(teacherId).orElseThrow().enabled, "新注册教师应等待审核");

            Repository reloaded = new AppRepository(directory);
            assertEquals("测试教师", reloaded.findProfile(teacherId).orElseThrow().realName);
            assertEquals(List.of("数学", "物理"), reloaded.findProfile(teacherId).orElseThrow().subjects);
            expectApiException(ErrorCode.AUTH_DISABLED, "已被禁用",
                    () -> new AuthService(reloaded).login(body(
                            "loginMethod", "PHONE",
                            "loginId", "13912345678",
                            "verificationCode", "000000"
                    )));

            String database = Files.readString(directory.resolve("ptagent-accounts.json"));
            assertTrue(database.contains("\"schemaVersion\":2"), "账号数据库应升级到schema v2");
            assertTrue(database.contains("\"loginMethod\":\"PHONE\""), "账号数据库应保存登录方式");
            assertTrue(!database.contains("passwordHash"), "账号数据库不得继续保存密码哈希");
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        } finally {
            if (directory != null) {
                try {
                    Files.deleteIfExists(directory.resolve("ptagent-accounts.json.tmp"));
                    Files.deleteIfExists(directory.resolve("ptagent-accounts.json"));
                    Files.deleteIfExists(directory);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }
    }

    private static void authRateLimitsPhoneAbuse() {
        Repository repository = new AppRepository(false);
        AuthService authService = new AuthService(repository);
        String issuedCode = String.valueOf(authService.issuePhoneCode("13800000003").get("demoCode"));
        String wrongCode = "000000".equals(issuedCode) ? "000001" : "000000";
        expectApiException(ErrorCode.AUTH_RATE_LIMITED, "发送过于频繁",
                () -> authService.issuePhoneCode("13800000003"));

        for (int attempt = 1; attempt < 5; attempt++) {
            expectApiException(ErrorCode.AUTH_VERIFICATION_CODE_INVALID, "验证码不正确",
                    () -> authService.login(body(
                            "loginMethod", "PHONE",
                            "loginId", "13800000003",
                            "verificationCode", wrongCode
                    )));
        }
        expectApiException(ErrorCode.AUTH_RATE_LIMITED, "错误次数过多",
                () -> authService.login(body(
                        "loginMethod", "PHONE",
                        "loginId", "13800000003",
                        "verificationCode", wrongCode
                )));
    }

    private static void apiRequestUsesAuthenticatedActor() {
        Repository repository = new AppRepository(false);
        User teacher = repository.findUser(102L).orElseThrow();
        ApiRequest request = new ApiRequest(
                "POST",
                List.of("demands"),
                Map.of("teacherId", "100"),
                body("adminId", 100L, "teacherId", 100L),
                "test-token",
                teacher
        );

        assertEquals(102L, number(request.bodyWithActor("adminId").get("adminId")));
        assertEquals("102", request.queryForActorRole("teacherId", teacher.role).get("teacherId"));
    }

    private static void demandFiltersAndScores() {
        Repository repository = new AppRepository(false);
        DemandService demandService = new DemandService(repository);

        List<Map<String, Object>> demands = demandService.listDemands(query(
                "subject", "数学",
                "teacherId", "102",
                "sort", "match"
        ));

        assertEquals(1, demands.size());
        Map<String, Object> demand = demands.get(0);
        assertEquals("数学", demand.get("subject"));
        assertEquals("OPEN", demand.get("status"));
        assertTrue(number(demand.get("matchScore")) >= 90, "数学老师对数学需求应获得高匹配分");
        assertTrue(String.valueOf(demand.get("parentPhone")).contains("****"), "教师浏览需求时家长电话应脱敏");
        assertTrue(!demand.containsKey("adminId"), "教师浏览需求时不应暴露发布管理员ID");
    }

    private static void httpSecurityBoundary() {
        HttpServer server = null;
        try {
            Repository repository = new AppRepository(false);
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api", new ApiRouter(repository));
            server.createContext("/", new StaticFileHandler("public"));
            server.start();

            int port = server.getAddress().getPort();
            String base = "http://127.0.0.1:" + port;
            HttpClient client = HttpClient.newHttpClient();

            HttpResponse<String> page = client.send(HttpRequest.newBuilder(URI.create(base + "/")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, page.statusCode());
            assertTrue(page.headers().firstValue("Content-Security-Policy").isPresent(),
                    "静态页面应包含CSP响应头");
            assertEquals("nosniff", page.headers().firstValue("X-Content-Type-Options").orElse(""));

            HttpResponse<String> hostileOrigin = client.send(HttpRequest.newBuilder(URI.create(base + "/api/bootstrap"))
                            .header("Origin", "https://evil.example").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertApiError(hostileOrigin, 403, ErrorCode.ORIGIN_NOT_ALLOWED);

            HttpResponse<String> sameOrigin = client.send(HttpRequest.newBuilder(URI.create(base + "/api/bootstrap"))
                            .header("Origin", base).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, sameOrigin.statusCode());
            assertEquals(base, sameOrigin.headers().firstValue("Access-Control-Allow-Origin").orElse(""));

            HttpResponse<String> wrongMedia = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
                            .header("Content-Type", "text/plain").POST(HttpRequest.BodyPublishers.ofString("{}"))
                            .build(), HttpResponse.BodyHandlers.ofString());
            assertApiError(wrongMedia, 415, ErrorCode.UNSUPPORTED_MEDIA_TYPE);

            String oversized = "{\"value\":\"" + "x".repeat(1024 * 1024) + "\"}";
            HttpResponse<String> oversizedResponse = postJson(client, base + "/api/auth/login", oversized, "");
            assertApiError(oversizedResponse, 413, ErrorCode.REQUEST_TOO_LARGE);

            HttpResponse<String> phoneCode = postJson(client, base + "/api/auth/phone-code",
                    "{\"phoneNumber\":\"13800000003\"}", "");
            assertEquals(200, phoneCode.statusCode());
            String demoCode = String.valueOf(castMap(Json.parseObject(phoneCode.body()).get("data")).get("demoCode"));
            HttpResponse<String> repeatedCode = postJson(client, base + "/api/auth/phone-code",
                    "{\"phoneNumber\":\"13800000003\"}", "");
            assertApiError(repeatedCode, 429, ErrorCode.AUTH_RATE_LIMITED);

            HttpResponse<String> login = postJson(client, base + "/api/auth/login",
                    Json.stringify(body("loginMethod", "PHONE", "loginId", "13800000003",
                            "verificationCode", demoCode)), "");
            String token = String.valueOf(castMap(Json.parseObject(login.body()).get("data")).get("token"));
            HttpResponse<String> demands = client.send(HttpRequest.newBuilder(URI.create(base + "/api/demands?status=OPEN"))
                            .header("Authorization", "Bearer " + token).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> demandItems = (List<Map<String, Object>>) Json.parseObject(demands.body()).get("data");
            assertTrue(!demandItems.isEmpty(), "教师应能读取开放需求");
            assertTrue(String.valueOf(demandItems.get(0).get("parentPhone")).contains("****"),
                    "教师需求响应应脱敏家长电话");
            assertTrue(!demandItems.get(0).containsKey("adminId"), "教师需求响应不应包含管理员ID");
        } catch (IOException | InterruptedException exception) {
            throw new RuntimeException(exception);
        } finally {
            if (server != null) {
                server.stop(0);
            }
        }
    }

    private static HttpResponse<String> postJson(HttpClient client, String url, String body, String token)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null && !token.isBlank()) {
            request.header("Authorization", "Bearer " + token);
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertApiError(HttpResponse<String> response, int status, ErrorCode code) {
        assertEquals(status, response.statusCode());
        assertEquals(code.name(), Json.parseObject(response.body()).get("code"));
    }

    private static void demandValidation() {
        Repository repository = new AppRepository(false);
        DemandService demandService = new DemandService(repository);

        expectApiException(ErrorCode.VALIDATION_ERROR, "家长姓名不能为空", () -> demandService.createDemand(body(
                "adminId", 101L,
                "parentPhone", "13900009999",
                "address", "天河区测试地址",
                "region", "天河区",
                "subject", "数学",
                "grade", "高一"
        )));
    }

    private static void demandXlsxImport() {
        Repository repository = new AppRepository(false);
        DemandImportService importService = new DemandImportService(repository);
        int before = repository.allDemands().size();

        Map<String, Object> result = importService.importDemandXlsx(Path.of("example.xlsx"), 101L);
        long importedCount = number(result.get("importedCount"));

        assertTrue(importedCount > 0, "example.xlsx应至少导入一条需求");
        assertEquals(before + importedCount, (long) repository.allDemands().size());
        assertTrue(repository.allDemands().stream().anyMatch(demand ->
                demand.remark != null && demand.remark.contains("订单号：")), "导入需求应保留订单号");

        Map<String, Object> duplicated = importService.importDemandXlsx(Path.of("example.xlsx"), 101L);
        assertEquals(0L, number(duplicated.get("importedCount")));
        assertTrue(number(duplicated.get("skippedCount")) >= importedCount, "重复导入应按订单号跳过");
    }

    private static void applicationDuplicateGuard() {
        Repository repository = new AppRepository(false);
        ApplicationService applicationService = new ApplicationService(repository);

        Map<String, Object> request = body(
                "teacherId", 102L,
                "selfIntro", "我可以按学生薄弱点安排针对性练习。"
        );

        applicationService.apply(300L, request);
        expectApiException(ErrorCode.APPLICATION_DUPLICATED, "你已申请过该需求",
                () -> applicationService.apply(300L, request));
    }

    private static void accessGuardRejectsTeacherAdminActions() {
        Repository repository = new AppRepository(false);
        DemandService demandService = new DemandService(repository);

        expectApiException(ErrorCode.ACCESS_DENIED, "permission denied", () -> demandService.createDemand(body(
                "adminId", 102L,
                "parentName", "Parent",
                "parentPhone", "13900009999",
                "address", "Test address",
                "region", "Test region",
                "subject", "Math",
                "grade", "Grade 10"
        )));
    }

    private static void applicationApprovalCreatesOrder() {
        Repository repository = new AppRepository(false);
        ApplicationService applicationService = new ApplicationService(repository);
        int orderCount = repository.allOrders().size();

        Map<String, Object> application = applicationService.apply(302L, body(
                "teacherId", 102L,
                "selfIntro", "我有初中物理力学专题辅导经验，可快速拆题。"
        ));
        long applicationId = number(application.get("id"));

        Map<String, Object> reviewed = applicationService.review(applicationId, body(
                "adminId", 100L,
                "status", "APPROVED"
        ));

        assertEquals("APPROVED", reviewed.get("status"));
        assertTrue(reviewed.containsKey("order"), "审核通过后应返回课程订单");
        assertEquals(orderCount + 1, repository.allOrders().size());
        assertEquals(DemandStatus.TEACHING, repository.findDemand(302L).orElseThrow().status);
        assertEquals(ApplicationStatus.APPROVED, repository.findApplication(applicationId).orElseThrow().status);

        @SuppressWarnings("unchecked")
        Map<String, Object> order = (Map<String, Object>) reviewed.get("order");
        assertEquals(302L, number(order.get("demandId")));
        assertEquals(102L, number(order.get("teacherId")));
    }

    private static void auditLogsCriticalOperations() {
        Repository repository = new AppRepository(false);
        ApplicationService applicationService = new ApplicationService(repository);
        AuditService auditService = new AuditService(repository);

        applicationService.apply(300L, body(
                "teacherId", 102L,
                "selfIntro", "Available for a staged tutoring plan."
        ));

        List<Map<String, Object>> logs = auditService.listAuditLogs(100L);
        assertTrue(logs.stream().anyMatch(log -> "APPLICATION_CREATE".equals(log.get("action"))),
                "application creation should be audited");
        expectApiException(ErrorCode.ACCESS_DENIED, "permission denied", () -> auditService.listAuditLogs(102L));
    }

    private static void courseRecordRequiresOrderTeacher() {
        Repository repository = new AppRepository(false);
        CourseService courseService = new CourseService(repository);

        expectApiException(ErrorCode.ACCESS_DENIED, "permission denied", () -> courseService.createRecord(500L, body(
                "teacherId", 102L,
                "lessonDate", "2026-06-26",
                "lessonDuration", 2,
                "content", "Lesson",
                "studentPerformance", "Good",
                "teacherNotes", "Next lesson"
        )));
    }

    private static Map<String, Object> body(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((String) pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static Map<String, String> query(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }

    private static long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new AssertionError("Expected number but got: " + value);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static void run(String name, TestCase test) {
        try {
            test.run();
            System.out.println("[PASS] " + name);
        } catch (RuntimeException | AssertionError e) {
            System.err.println("[FAIL] " + name);
            throw e;
        }
    }

    private static void expectApiException(ErrorCode code, String messagePart, TestCase test) {
        try {
            test.run();
        } catch (ApiException e) {
            assertEquals(code, e.code());
            assertTrue(e.getMessage().contains(messagePart),
                    "Expected error containing " + messagePart + " but got " + e.getMessage());
            return;
        }
        throw new AssertionError("Expected ApiException " + code + " containing: " + messagePart);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface TestCase {
        void run();
    }
}

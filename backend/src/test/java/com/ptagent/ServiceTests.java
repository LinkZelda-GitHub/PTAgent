package com.ptagent;

import com.ptagent.domain.ApplicationStatus;
import com.ptagent.domain.DemandStatus;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceTests {
    public static void main(String[] args) {
        run("AuthService returns stable invalid password code", ServiceTests::authInvalidPasswordCode);
        run("AuthService creates, restores and revokes sessions", ServiceTests::authSessionLifecycle);
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

    private static void authInvalidPasswordCode() {
        Repository repository = new AppRepository(false);
        AuthService authService = new AuthService(repository);

        expectApiException(ErrorCode.AUTH_INVALID_PASSWORD, "密码不正确",
                () -> authService.login("teacher", "wrong-password"));
    }

    private static void authSessionLifecycle() {
        Repository repository = new AppRepository(false);
        AuthService authService = new AuthService(repository);

        Map<String, Object> login = authService.login("teacher", "teacher123");
        String token = String.valueOf(login.get("token"));
        assertTrue(!token.isBlank(), "登录应签发会话令牌");

        @SuppressWarnings("unchecked")
        Map<String, Object> currentUser = (Map<String, Object>) authService.currentSession(token).get("user");
        assertEquals("teacher", currentUser.get("username"));

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
            Map<String, Object> registered = authService.registerTeacher(body(
                    "username", "new_teacher",
                    "password", "Strong123",
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
            expectApiException(ErrorCode.AUTH_DISABLED, "账号已被禁用",
                    () -> new AuthService(reloaded).login("new_teacher", "Strong123"));

            String database = Files.readString(directory.resolve("ptagent-accounts.json"));
            assertTrue(database.contains("passwordHash"), "账号数据库应保存密码哈希");
            assertTrue(!database.contains("Strong123"), "账号数据库不得保存明文密码");
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

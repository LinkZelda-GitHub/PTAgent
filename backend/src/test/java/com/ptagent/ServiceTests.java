package com.ptagent;

import com.ptagent.domain.ApplicationStatus;
import com.ptagent.domain.DemandStatus;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.AppRepository;
import com.ptagent.repository.Repository;
import com.ptagent.service.ApplicationService;
import com.ptagent.service.AuthService;
import com.ptagent.service.DemandImportService;
import com.ptagent.service.DemandService;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceTests {
    public static void main(String[] args) {
        run("AuthService returns stable invalid password code", ServiceTests::authInvalidPasswordCode);
        run("DemandService filters by subject and calculates match score", ServiceTests::demandFiltersAndScores);
        run("DemandService rejects invalid demand payloads", ServiceTests::demandValidation);
        run("DemandImportService imports xlsx order format", ServiceTests::demandXlsxImport);
        run("ApplicationService blocks duplicated applications", ServiceTests::applicationDuplicateGuard);
        run("ApplicationService approval creates order and updates demand", ServiceTests::applicationApprovalCreatesOrder);
        System.out.println("All service tests passed.");
    }

    private static void authInvalidPasswordCode() {
        Repository repository = new AppRepository();
        AuthService authService = new AuthService(repository);

        expectApiException(ErrorCode.AUTH_INVALID_PASSWORD, "密码不正确",
                () -> authService.login("teacher", "wrong-password"));
    }

    private static void demandFiltersAndScores() {
        Repository repository = new AppRepository();
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
        Repository repository = new AppRepository();
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
        Repository repository = new AppRepository();
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
        Repository repository = new AppRepository();
        ApplicationService applicationService = new ApplicationService(repository);

        Map<String, Object> request = body(
                "teacherId", 102L,
                "selfIntro", "我可以按学生薄弱点安排针对性练习。"
        );

        applicationService.apply(300L, request);
        expectApiException(ErrorCode.APPLICATION_DUPLICATED, "你已申请过该需求",
                () -> applicationService.apply(300L, request));
    }

    private static void applicationApprovalCreatesOrder() {
        Repository repository = new AppRepository();
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

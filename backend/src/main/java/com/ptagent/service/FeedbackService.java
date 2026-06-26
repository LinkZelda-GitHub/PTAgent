package com.ptagent.service;

import com.ptagent.common.Json;
import com.ptagent.domain.CourseOrder;
import com.ptagent.domain.Feedback;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class FeedbackService {
    private final Repository repository;
    private final AccessGuard accessGuard;

    public FeedbackService(Repository repository) {
        this.repository = repository;
        this.accessGuard = new AccessGuard(repository);
    }

    public List<Map<String, Object>> listFeedbacks() {
        return repository.allFeedbacks().stream()
                .sorted(Comparator.comparing((Feedback feedback) -> feedback.submitTime).reversed())
                .map(this::toMap)
                .toList();
    }

    public Map<String, Object> createFeedback(Map<String, Object> body) {
        long orderId = Json.longValue(body, "orderId", 0);
        long adminId = Json.longValue(body, "submitAdminId", 0);
        accessGuard.requireAdmin(adminId);
        int score = (int) Json.longValue(body, "ratingScore", 5);
        if (score < 1 || score > 5) {
            throw ApiException.badRequest(ErrorCode.FEEDBACK_INVALID_SCORE, "评分必须在1-5之间");
        }
        repository.findOrder(orderId).orElseThrow(() -> ApiException.notFound(ErrorCode.ORDER_NOT_FOUND, "课程订单不存在"));
        Feedback feedback = repository.createFeedback(orderId, score, Json.str(body, "commentText"),
                (int) Json.longValue(body, "feedbackSource", 1),
                adminId);
        repository.createAuditLog(adminId, "FEEDBACK_CREATE", "ORDER", orderId,
                "feedbackId=" + feedback.id);
        return toMap(feedback);
    }

    private Map<String, Object> toMap(Feedback feedback) {
        CourseOrder order = repository.findOrder(feedback.orderId).orElse(null);
        return feedback.toMap(order, order == null ? null : repository.findProfile(order.teacherId).orElse(null));
    }
}

package com.ptagent.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class Feedback {
    public long id;
    public long orderId;
    public int ratingScore;
    public String commentText;
    public int feedbackSource = 1;
    public LocalDateTime submitTime = LocalDateTime.now();
    public long submitAdminId;

    public Map<String, Object> toMap(CourseOrder order, TeacherProfile teacher) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("orderId", orderId);
        map.put("ratingScore", ratingScore);
        map.put("commentText", commentText);
        map.put("feedbackSource", feedbackSource);
        map.put("feedbackSourceLabel", feedbackSource == 1 ? "家长回访" : "教师自评");
        map.put("submitTime", submitTime == null ? null : submitTime.toString());
        map.put("submitAdminId", submitAdminId);
        map.put("teacherId", order == null ? 0 : order.teacherId);
        map.put("teacherName", teacher == null ? "" : teacher.realName);
        return map;
    }
}

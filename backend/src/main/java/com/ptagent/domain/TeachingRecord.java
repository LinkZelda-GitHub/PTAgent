package com.ptagent.domain;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class TeachingRecord {
    public long id;
    public long orderId;
    public LocalDate lessonDate = LocalDate.now();
    public double lessonDuration;
    public String content;
    public String studentPerformance;
    public String teacherNotes;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("orderId", orderId);
        map.put("lessonDate", lessonDate == null ? null : lessonDate.toString());
        map.put("lessonDuration", lessonDuration);
        map.put("content", content);
        map.put("studentPerformance", studentPerformance);
        map.put("teacherNotes", teacherNotes);
        return map;
    }
}

package com.ptagent.service;

import com.ptagent.common.Json;
import com.ptagent.domain.CourseOrder;
import com.ptagent.domain.TeachingRecord;
import com.ptagent.repository.AppRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CourseService {
    private final AppRepository repository;

    public CourseService(AppRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> listOrders(long teacherId) {
        return repository.allOrders().stream()
                .filter(order -> teacherId == 0 || order.teacherId == teacherId)
                .sorted(Comparator.comparing((CourseOrder order) -> order.createTime).reversed())
                .map(order -> order.toMap(repository.findDemand(order.demandId).orElse(null),
                        repository.findProfile(order.teacherId).orElse(null)))
                .toList();
    }

    public List<Map<String, Object>> listRecords(long orderId) {
        return repository.allTeachingRecords().stream()
                .filter(record -> orderId == 0 || record.orderId == orderId)
                .sorted(Comparator.comparing((TeachingRecord record) -> record.lessonDate).reversed())
                .map(TeachingRecord::toMap)
                .toList();
    }

    public Map<String, Object> createRecord(long orderId, Map<String, Object> body) {
        repository.findOrder(orderId).orElseThrow(() -> new IllegalArgumentException("课程订单不存在"));
        LocalDate lessonDate = Json.str(body, "lessonDate").isBlank()
                ? LocalDate.now() : LocalDate.parse(Json.str(body, "lessonDate"));
        TeachingRecord record = repository.createTeachingRecord(orderId,
                lessonDate,
                Json.doubleValue(body, "lessonDuration", 1.5),
                Json.str(body, "content"),
                Json.str(body, "studentPerformance"),
                Json.str(body, "teacherNotes"));
        return record.toMap();
    }
}

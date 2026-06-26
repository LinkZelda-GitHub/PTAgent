package com.ptagent.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class CourseOrder {
    public long id;
    public long demandId;
    public long teacherId;
    public long adminId;
    public OrderStatus status = OrderStatus.WAITING;
    public LocalDateTime createTime = LocalDateTime.now();
    public LocalDateTime finishTime;
    public boolean contactShared = true;

    public Map<String, Object> toMap(Demand demand, TeacherProfile teacher) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("demandId", demandId);
        map.put("teacherId", teacherId);
        map.put("adminId", adminId);
        map.put("status", status.name());
        map.put("statusLabel", status.label);
        map.put("createTime", createTime == null ? null : createTime.toString());
        map.put("finishTime", finishTime == null ? null : finishTime.toString());
        map.put("contactShared", contactShared);
        map.put("courseTitle", demand == null ? "" : demand.grade + demand.subject);
        map.put("address", demand == null ? "" : demand.address);
        map.put("parentName", demand == null ? "" : demand.parentName);
        map.put("parentPhone", demand == null ? "" : demand.parentPhone);
        map.put("parentWechat", demand == null ? "" : demand.parentWechat);
        map.put("teacherName", teacher == null ? "" : teacher.realName);
        map.put("teacherPhone", teacher == null ? "" : teacher.contactPhone);
        map.put("teacherWechat", teacher == null ? "" : teacher.contactWechat);
        return map;
    }
}

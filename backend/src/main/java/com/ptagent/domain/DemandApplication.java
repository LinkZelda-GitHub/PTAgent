package com.ptagent.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class DemandApplication {
    public long id;
    public long demandId;
    public long teacherId;
    public String selfIntro;
    public LocalDateTime applyTime = LocalDateTime.now();
    public ApplicationStatus status = ApplicationStatus.PENDING;
    public LocalDateTime confirmTime;
    public long confirmAdminId;

    public Map<String, Object> toMap(Demand demand, TeacherProfile teacher) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("demandId", demandId);
        map.put("teacherId", teacherId);
        map.put("teacherName", teacher == null ? "" : teacher.realName);
        map.put("teacherTags", teacher == null ? java.util.List.of() : teacher.tags());
        map.put("selfIntro", selfIntro);
        map.put("applyTime", applyTime == null ? null : applyTime.toString());
        map.put("status", status.name());
        map.put("statusLabel", status.label);
        map.put("confirmTime", confirmTime == null ? null : confirmTime.toString());
        map.put("confirmAdminId", confirmAdminId);
        map.put("demandTitle", demand == null ? "" : demand.grade + demand.subject);
        map.put("demandAddress", demand == null ? "" : demand.address);
        return map;
    }
}

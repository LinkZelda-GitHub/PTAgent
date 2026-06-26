package com.ptagent.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class TeacherResume {
    public long id;
    public long teacherId;
    public String fileUrl;
    public String summary;
    public boolean active = true;
    public LocalDateTime submitTime = LocalDateTime.now();
    public int status;

    public String statusLabel() {
        return switch (status) {
            case 1 -> "已查看";
            case 2 -> "已筛选";
            default -> "待查看";
        };
    }

    public Map<String, Object> toMap(TeacherProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("teacherId", teacherId);
        map.put("teacherName", profile == null ? "" : profile.realName);
        map.put("fileUrl", fileUrl);
        map.put("summary", summary);
        map.put("active", active);
        map.put("submitTime", submitTime == null ? null : submitTime.toString());
        map.put("status", status);
        map.put("statusLabel", statusLabel());
        return map;
    }
}

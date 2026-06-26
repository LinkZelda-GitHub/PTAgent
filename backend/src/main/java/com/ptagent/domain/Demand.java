package com.ptagent.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Demand {
    public long id;
    public long adminId;
    public String parentName;
    public String parentPhone;
    public String parentWechat;
    public String address;
    public String region;
    public double longitude;
    public double latitude;
    public String subject;
    public String grade;
    public int teacherGender = 3;
    public String basicScore;
    public String salaryRange;
    public int salaryMin;
    public int salaryMax;
    public String remark;
    public List<String> qualificationTags = new ArrayList<>();
    public boolean is985Required;
    public boolean is211Required;
    public boolean isKeyUniversityRequired;
    public DemandStatus status = DemandStatus.OPEN;
    public LocalDateTime createTime = LocalDateTime.now();
    public LocalDateTime closeTime;

    public List<String> allTags() {
        List<String> tags = new ArrayList<>(qualificationTags);
        if (is985Required && !tags.contains("985")) {
            tags.add("985");
        }
        if (is211Required && !tags.contains("211")) {
            tags.add("211");
        }
        if (isKeyUniversityRequired && !tags.contains("重本")) {
            tags.add("重本");
        }
        return tags;
    }

    public String teacherGenderLabel() {
        return switch (teacherGender) {
            case 1 -> "男教师";
            case 2 -> "女教师";
            default -> "不限";
        };
    }

    public Map<String, Object> toMap(double distanceKm, int matchScore) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("adminId", adminId);
        map.put("parentName", parentName);
        map.put("parentPhone", parentPhone);
        map.put("parentWechat", parentWechat);
        map.put("address", address);
        map.put("region", region);
        map.put("longitude", longitude);
        map.put("latitude", latitude);
        map.put("subject", subject);
        map.put("grade", grade);
        map.put("teacherGender", teacherGender);
        map.put("teacherGenderLabel", teacherGenderLabel());
        map.put("basicScore", basicScore);
        map.put("salaryRange", salaryRange);
        map.put("salaryMin", salaryMin);
        map.put("salaryMax", salaryMax);
        map.put("remark", remark);
        map.put("qualificationTags", allTags());
        map.put("is985Required", is985Required);
        map.put("is211Required", is211Required);
        map.put("isKeyUniversityRequired", isKeyUniversityRequired);
        map.put("status", status.name());
        map.put("statusLabel", status.label);
        map.put("createTime", createTime == null ? null : createTime.toString());
        map.put("closeTime", closeTime == null ? null : closeTime.toString());
        map.put("distanceKm", distanceKm);
        map.put("matchScore", matchScore);
        return map;
    }
}

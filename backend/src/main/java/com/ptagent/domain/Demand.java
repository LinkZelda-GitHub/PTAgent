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
        return toMap(distanceKm, matchScore, true);
    }

    public Map<String, Object> toMap(double distanceKm, int matchScore, boolean includePrivateDetails) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        if (includePrivateDetails) {
            map.put("adminId", adminId);
        }
        map.put("parentName", includePrivateDetails ? parentName : maskedName(parentName));
        map.put("parentPhone", includePrivateDetails ? parentPhone : maskedPhone(parentPhone));
        map.put("parentWechat", includePrivateDetails ? parentWechat : "");
        map.put("address", includePrivateDetails ? address : region + "（详细地址匹配后可见）");
        map.put("region", region);
        map.put("longitude", includePrivateDetails ? longitude : approximateCoordinate(longitude));
        map.put("latitude", includePrivateDetails ? latitude : approximateCoordinate(latitude));
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

    private String maskedName(String value) {
        if (value == null || value.isBlank()) {
            return "家长";
        }
        return value.substring(0, 1) + "**";
    }

    private String maskedPhone(String value) {
        if (value == null || !value.matches("1[3-9]\\d{9}")) {
            return "匹配后可见";
        }
        return value.substring(0, 3) + "****" + value.substring(7);
    }

    private double approximateCoordinate(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}

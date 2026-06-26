package com.ptagent.service;

import com.ptagent.common.Json;
import com.ptagent.domain.Demand;
import com.ptagent.domain.DemandStatus;
import com.ptagent.domain.TeacherProfile;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class DemandService {
    private static final double DEFAULT_TEACHER_LAT = 23.1291;
    private static final double DEFAULT_TEACHER_LON = 113.2644;

    private final Repository repository;
    private final AccessGuard accessGuard;

    public DemandService(Repository repository) {
        this.repository = repository;
        this.accessGuard = new AccessGuard(repository);
    }

    public List<Map<String, Object>> listDemands(Map<String, String> query) {
        long teacherId = parseLong(query.get("teacherId"), 0);
        TeacherProfile profile = teacherId == 0 ? null : repository.findProfile(teacherId).orElse(null);
        String subject = text(query.get("subject"));
        String grade = text(query.get("grade"));
        String region = text(query.get("region"));
        String gender = text(query.get("gender"));
        String tag = text(query.get("tag"));
        String score = text(query.get("score"));
        String status = text(query.getOrDefault("status", "OPEN"));
        int minSalary = (int) parseLong(query.get("salaryMin"), 0);
        int maxSalary = (int) parseLong(query.get("salaryMax"), 0);

        List<DemandProjection> projections = new ArrayList<>();
        for (Demand demand : repository.allDemands()) {
            if (!status.equalsIgnoreCase("ALL") && !demand.status.name().equalsIgnoreCase(status)) {
                continue;
            }
            if (!subject.isBlank() && !demand.subject.equals(subject)) {
                continue;
            }
            if (!grade.isBlank() && !demand.grade.equals(grade)) {
                continue;
            }
            if (!region.isBlank() && !demand.region.equals(region)) {
                continue;
            }
            if (!gender.isBlank() && demand.teacherGender != Integer.parseInt(gender)) {
                continue;
            }
            if (!tag.isBlank() && !demand.allTags().contains(tag)) {
                continue;
            }
            String scoreText = demand.basicScore == null ? "" : demand.basicScore;
            if (!score.isBlank() && !"不限".equals(score) && !scoreText.contains(score)) {
                continue;
            }
            if (minSalary > 0 && demand.salaryMax < minSalary) {
                continue;
            }
            if (maxSalary > 0 && demand.salaryMin > maxSalary) {
                continue;
            }
            double distance = distanceKm(DEFAULT_TEACHER_LAT, DEFAULT_TEACHER_LON, demand.latitude, demand.longitude);
            int match = matchScore(profile, demand, distance);
            projections.add(new DemandProjection(demand, distance, match));
        }

        String sort = query.getOrDefault("sort", "latest");
        Comparator<DemandProjection> comparator = switch (sort) {
            case "distance" -> Comparator.comparingDouble(item -> item.distanceKm);
            case "salaryHigh" -> Comparator.comparingInt((DemandProjection item) -> item.demand.salaryMax).reversed();
            case "salaryLow" -> Comparator.comparingInt(item -> item.demand.salaryMin);
            case "match" -> Comparator.comparingInt((DemandProjection item) -> item.matchScore).reversed();
            default -> Comparator.comparing((DemandProjection item) -> item.demand.createTime).reversed();
        };
        return projections.stream()
                .sorted(comparator)
                .map(item -> item.demand.toMap(round1(item.distanceKm), item.matchScore))
                .toList();
    }

    public Map<String, Object> getDemand(long id, long teacherId) {
        Demand demand = repository.findDemand(id)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.DEMAND_NOT_FOUND, "需求不存在"));
        TeacherProfile profile = repository.findProfile(teacherId).orElse(null);
        double distance = distanceKm(DEFAULT_TEACHER_LAT, DEFAULT_TEACHER_LON, demand.latitude, demand.longitude);
        return demand.toMap(round1(distance), matchScore(profile, demand, distance));
    }

    public Map<String, Object> createDemand(Map<String, Object> body) {
        Demand demand = new Demand();
        demand.adminId = Json.longValue(body, "adminId", 0);
        accessGuard.requireAdmin(demand.adminId);
        demand.parentName = required(body, "parentName", "家长姓名不能为空");
        demand.parentPhone = required(body, "parentPhone", "家长电话不能为空");
        demand.parentWechat = Json.str(body, "parentWechat");
        demand.address = required(body, "address", "地址不能为空");
        demand.region = required(body, "region", "区域不能为空");
        demand.longitude = Json.doubleValue(body, "longitude", DEFAULT_TEACHER_LON);
        demand.latitude = Json.doubleValue(body, "latitude", DEFAULT_TEACHER_LAT);
        demand.subject = required(body, "subject", "科目不能为空");
        demand.grade = required(body, "grade", "年级不能为空");
        demand.teacherGender = (int) Json.longValue(body, "teacherGender", 3);
        if (demand.teacherGender < 1 || demand.teacherGender > 3) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "教师性别要求不正确");
        }
        demand.basicScore = Json.str(body, "basicScore");
        demand.salaryMin = (int) Json.longValue(body, "salaryMin", 0);
        demand.salaryMax = (int) Json.longValue(body, "salaryMax", 0);
        if (demand.salaryMin < 0 || demand.salaryMax < 0 || (demand.salaryMin > 0 && demand.salaryMax > 0
                && demand.salaryMin > demand.salaryMax)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "薪酬范围不正确");
        }
        demand.salaryRange = Json.str(body, "salaryRange");
        if (demand.salaryRange.isBlank() && demand.salaryMin > 0 && demand.salaryMax > 0) {
            demand.salaryRange = demand.salaryMin + "-" + demand.salaryMax + "元/小时";
        }
        demand.remark = Json.str(body, "remark");
        demand.qualificationTags = Json.stringList(body, "qualificationTags");
        demand.is985Required = Json.bool(body, "is985Required", false);
        demand.is211Required = Json.bool(body, "is211Required", false);
        demand.isKeyUniversityRequired = Json.bool(body, "isKeyUniversityRequired", false);
        Demand created = repository.createDemand(demand);
        repository.createAuditLog(demand.adminId, "DEMAND_CREATE", "DEMAND", created.id,
                created.grade + created.subject);
        return created.toMap(round1(distanceKm(DEFAULT_TEACHER_LAT, DEFAULT_TEACHER_LON, created.latitude, created.longitude)), 0);
    }

    public Map<String, Object> closeDemand(long demandId, Map<String, Object> body) {
        long adminId = Json.longValue(body, "adminId", 0);
        accessGuard.requireAdmin(adminId);
        Demand demand = repository.findDemand(demandId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.DEMAND_NOT_FOUND, "需求不存在"));
        demand.status = DemandStatus.CLOSED;
        demand.closeTime = LocalDateTime.now();
        repository.saveDemand(demand);
        repository.createAuditLog(adminId, "DEMAND_CLOSE", "DEMAND", demand.id,
                demand.grade + demand.subject);
        return demand.toMap(round1(distanceKm(DEFAULT_TEACHER_LAT, DEFAULT_TEACHER_LON, demand.latitude, demand.longitude)), 0);
    }

    private int matchScore(TeacherProfile profile, Demand demand, double distanceKm) {
        if (profile == null) {
            return 0;
        }
        int score = 0;
        if (profile.subjects.contains(demand.subject)) {
            score += 40;
        }
        if (fitsGrade(profile, demand.grade)) {
            score += 25;
        }
        List<String> teacherTags = profile.tags();
        List<String> demandTags = demand.allTags();
        if (!demandTags.isEmpty()) {
            long hits = demandTags.stream().filter(teacherTags::contains).count();
            score += (int) Math.round(20.0 * hits / demandTags.size());
        } else {
            score += 20;
        }
        if (distanceKm <= 3) {
            score += 15;
        } else if (distanceKm <= 8) {
            score += 10;
        } else if (distanceKm <= 15) {
            score += 5;
        }
        return Math.min(100, score);
    }

    private boolean fitsGrade(TeacherProfile profile, String grade) {
        if (profile.subjects.contains("奥数") && grade.contains("小学")) {
            return true;
        }
        if (profile.subjects.contains("物理") && (grade.contains("初") || grade.contains("高"))) {
            return true;
        }
        if ((profile.subjects.contains("英语") || profile.subjects.contains("语文")) && !grade.contains("高三")) {
            return true;
        }
        return profile.teachingExperience != null && profile.teachingExperience.contains(grade.substring(0, Math.min(2, grade.length())));
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String required(Map<String, Object> body, String key, String message) {
        String value = Json.str(body, key);
        if (value.isBlank()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, message);
        }
        return value;
    }

    private record DemandProjection(Demand demand, double distanceKm, int matchScore) {
    }
}

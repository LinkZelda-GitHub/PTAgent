package com.ptagent.service;

import com.ptagent.common.Json;
import com.ptagent.domain.RoleType;
import com.ptagent.domain.TeacherProfile;
import com.ptagent.domain.TeacherResume;
import com.ptagent.domain.User;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TeacherService {
    private final Repository repository;
    private final AccessGuard accessGuard;

    public TeacherService(Repository repository) {
        this.repository = repository;
        this.accessGuard = new AccessGuard(repository);
    }

    public List<Map<String, Object>> listTeachers() {
        return repository.allProfiles().stream()
                .map(profile -> profile.toMap(repository.findUser(profile.teacherId).orElse(null)))
                .sorted(Comparator.comparing(map -> String.valueOf(map.get("realName"))))
                .toList();
    }

    public Map<String, Object> updateProfile(long teacherId, Map<String, Object> body) {
        long actorId = Json.longValue(body, "actorId", teacherId);
        accessGuard.requireSelfOrAdmin(actorId, teacherId);
        TeacherProfile profile = repository.findProfile(teacherId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.TEACHER_PROFILE_NOT_FOUND, "教师资料不存在"));
        if (body.containsKey("realName")) {
            profile.realName = Json.str(body, "realName");
        }
        if (body.containsKey("gender")) {
            profile.gender = (int) Json.longValue(body, "gender", profile.gender);
        }
        if (body.containsKey("education")) {
            profile.education = Json.str(body, "education");
        }
        if (body.containsKey("graduateSchool")) {
            profile.graduateSchool = Json.str(body, "graduateSchool");
        }
        if (body.containsKey("subjects")) {
            profile.subjects = Json.stringList(body, "subjects");
        }
        if (body.containsKey("teachingExperience")) {
            profile.teachingExperience = Json.str(body, "teachingExperience");
        }
        if (body.containsKey("expectedRate")) {
            profile.expectedRate = Json.str(body, "expectedRate");
        }
        if (body.containsKey("serviceArea")) {
            profile.serviceArea = Json.stringList(body, "serviceArea");
        }
        if (body.containsKey("personalIntro")) {
            profile.personalIntro = Json.str(body, "personalIntro");
        }
        profile.is985 = Json.bool(body, "is985", profile.is985);
        profile.is211 = Json.bool(body, "is211", profile.is211);
        profile.isKeyUniversity = Json.bool(body, "isKeyUniversity", profile.isKeyUniversity);
        profile.hasTeacherCert = Json.bool(body, "hasTeacherCert", profile.hasTeacherCert);
        profile.normalUniversity = Json.bool(body, "normalUniversity", profile.normalUniversity);
        profile.competitionExperience = Json.bool(body, "competitionExperience", profile.competitionExperience);
        repository.saveProfile(profile);
        repository.createAuditLog(actorId, "TEACHER_PROFILE_UPDATE", "TEACHER", teacherId, profile.realName);
        return profile.toMap(repository.findUser(teacherId).orElse(null));
    }

    public Map<String, Object> setEnabled(long teacherId, boolean enabled, long adminId) {
        accessGuard.requireSuperAdmin(adminId);
        User user = repository.findUser(teacherId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        if (user.role != RoleType.TEACHER) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "只能审核教师账号");
        }
        user.enabled = enabled;
        repository.saveUser(user);
        repository.createAuditLog(adminId, enabled ? "TEACHER_ENABLE" : "TEACHER_DISABLE", "TEACHER", teacherId,
                user.username);
        repository.createNotification(teacherId, enabled ? "账号已启用" : "账号已禁用",
                enabled ? "最高管理员已启用你的教师账号。" : "账号已被禁用，请联系平台管理员。");
        TeacherProfile profile = repository.findProfile(teacherId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.TEACHER_PROFILE_NOT_FOUND, "教师资料不存在"));
        return profile.toMap(user);
    }

    public List<Map<String, Object>> listResumes() {
        return repository.allResumes().stream()
                .sorted(Comparator.comparing((TeacherResume resume) -> resume.submitTime).reversed())
                .map(resume -> resume.toMap(repository.findProfile(resume.teacherId).orElse(null)))
                .toList();
    }

    public Map<String, Object> submitResume(Map<String, Object> body) {
        long teacherId = Json.longValue(body, "teacherId", 0);
        long actorId = Json.longValue(body, "actorId", teacherId);
        accessGuard.requireSelfOrAdmin(actorId, teacherId);
        accessGuard.requireTeacher(teacherId);
        if (teacherId == 0 || repository.findProfile(teacherId).isEmpty()) {
            throw ApiException.notFound(ErrorCode.TEACHER_NOT_FOUND, "教师不存在");
        }
        TeacherResume resume = repository.createResume(teacherId, Json.str(body, "fileUrl"), Json.str(body, "summary"));
        repository.createAuditLog(actorId, "RESUME_SUBMIT", "RESUME", resume.id, resume.summary);
        repository.createNotification(teacherId, "简历已提交", "平台管理员已收到你的最新简历。");
        return resume.toMap(repository.findProfile(teacherId).orElse(null));
    }

    public Map<String, Object> updateResumeStatus(long resumeId, int status, long adminId) {
        accessGuard.requireAdmin(adminId);
        TeacherResume resume = repository.findResume(resumeId)
                .orElseThrow(() -> ApiException.notFound(ErrorCode.RESUME_NOT_FOUND, "简历不存在"));
        resume.status = status;
        repository.createAuditLog(adminId, "RESUME_STATUS_UPDATE", "RESUME", resume.id,
                "status=" + status);
        return resume.toMap(repository.findProfile(resume.teacherId).orElse(null));
    }
}

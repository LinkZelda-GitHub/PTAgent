package com.ptagent.service;

import com.ptagent.common.Json;
import com.ptagent.domain.RoleType;
import com.ptagent.domain.TeacherProfile;
import com.ptagent.domain.TeacherResume;
import com.ptagent.domain.User;
import com.ptagent.repository.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class TeacherService {
    private final Repository repository;

    public TeacherService(Repository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> listTeachers() {
        return repository.allProfiles().stream()
                .map(profile -> profile.toMap(repository.findUser(profile.teacherId).orElse(null)))
                .sorted(Comparator.comparing(map -> String.valueOf(map.get("realName"))))
                .toList();
    }

    public Map<String, Object> updateProfile(long teacherId, Map<String, Object> body) {
        TeacherProfile profile = repository.findProfile(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("教师资料不存在"));
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
        return profile.toMap(repository.findUser(teacherId).orElse(null));
    }

    public Map<String, Object> setEnabled(long teacherId, boolean enabled) {
        User user = repository.findUser(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (user.role != RoleType.TEACHER) {
            throw new IllegalArgumentException("只能审核教师账号");
        }
        user.enabled = enabled;
        repository.saveUser(user);
        repository.createNotification(teacherId, enabled ? "账号已启用" : "账号已禁用",
                enabled ? "最高管理员已启用你的教师账号。" : "账号已被禁用，请联系平台管理员。");
        TeacherProfile profile = repository.findProfile(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("教师资料不存在"));
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
        if (teacherId == 0 || repository.findProfile(teacherId).isEmpty()) {
            throw new IllegalArgumentException("教师不存在");
        }
        TeacherResume resume = repository.createResume(teacherId, Json.str(body, "fileUrl"), Json.str(body, "summary"));
        repository.createNotification(teacherId, "简历已提交", "平台管理员已收到你的最新简历。");
        return resume.toMap(repository.findProfile(teacherId).orElse(null));
    }

    public Map<String, Object> updateResumeStatus(long resumeId, int status) {
        TeacherResume resume = repository.findResume(resumeId)
                .orElseThrow(() -> new IllegalArgumentException("简历不存在"));
        resume.status = status;
        return resume.toMap(repository.findProfile(resume.teacherId).orElse(null));
    }
}

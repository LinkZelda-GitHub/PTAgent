package com.ptagent.service;

import com.ptagent.common.Json;
import com.ptagent.common.Passwords;
import com.ptagent.domain.RoleType;
import com.ptagent.domain.TeacherProfile;
import com.ptagent.domain.User;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.Repository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuthService {
    private final Repository repository;

    public AuthService(Repository repository) {
        this.repository = repository;
    }

    public Map<String, Object> login(String username, String password) {
        User user = repository.findUserByUsername(username)
                .orElseThrow(() -> ApiException.badRequest(ErrorCode.AUTH_USER_NOT_FOUND, "账号不存在"));
        if (!user.enabled) {
            throw ApiException.badRequest(ErrorCode.AUTH_DISABLED, "账号已被禁用");
        }
        if (!Passwords.matches(password, user.passwordHash)) {
            throw ApiException.badRequest(ErrorCode.AUTH_INVALID_PASSWORD, "密码不正确");
        }
        user.lastLogin = LocalDateTime.now();
        repository.saveUser(user);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("user", user.toPublicMap());
        repository.findProfile(user.id).ifPresent(profile -> response.put("profile", profile.toMap(user)));
        return response;
    }

    public Map<String, Object> registerTeacher(Map<String, Object> body) {
        String username = required(body, "username", "用户名不能为空");
        String password = required(body, "password", "密码不能为空");
        if (repository.findUserByUsername(username).isPresent()) {
            throw ApiException.badRequest(ErrorCode.AUTH_USERNAME_EXISTS, "用户名已存在");
        }
        User user = repository.createUser(username, password, RoleType.TEACHER,
                Json.str(body, "phoneNumber"), Json.str(body, "email"), false);

        TeacherProfile profile = new TeacherProfile();
        profile.teacherId = user.id;
        profile.realName = Json.str(body, "realName").isBlank() ? username : Json.str(body, "realName");
        profile.gender = (int) Json.longValue(body, "gender", 1);
        profile.education = Json.str(body, "education");
        profile.graduateSchool = Json.str(body, "graduateSchool");
        profile.subjects = Json.stringList(body, "subjects");
        profile.contactPhone = Json.str(body, "phoneNumber");
        profile.contactWechat = Json.str(body, "contactWechat");
        profile.personalIntro = Json.str(body, "personalIntro");
        repository.saveProfile(profile);
        repository.createNotification(user.id, "账号待审核", "教师账号已注册，请等待最高管理员启用。");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("user", user.toPublicMap());
        response.put("profile", profile.toMap(user));
        return response;
    }

    private String required(Map<String, Object> body, String key, String message) {
        String value = Json.str(body, key);
        if (value.isBlank()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, message);
        }
        return value;
    }
}

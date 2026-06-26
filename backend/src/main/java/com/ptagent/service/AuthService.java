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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private static final int SESSION_HOURS = 12;
    private final Repository repository;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

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
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_HOURS);
        sessions.put(token, new Session(user.id, expiresAt));

        Map<String, Object> response = sessionResponse(user);
        response.put("token", token);
        response.put("expiresAt", expiresAt.toString());
        return response;
    }

    public Map<String, Object> currentSession(String token) {
        return sessionResponse(requireSession(token));
    }

    public User requireSession(String token) {
        if (token == null || token.isBlank()) {
            throw new ApiException(401, ErrorCode.AUTH_REQUIRED, "请先登录");
        }
        Session session = sessions.get(token);
        if (session == null || session.expiresAt().isBefore(LocalDateTime.now())) {
            sessions.remove(token);
            throw new ApiException(401, ErrorCode.AUTH_SESSION_INVALID, "登录状态已失效，请重新登录");
        }
        User user = repository.findUser(session.userId())
                .orElseThrow(() -> new ApiException(401, ErrorCode.AUTH_SESSION_INVALID, "登录账号不存在"));
        if (!user.enabled) {
            sessions.remove(token);
            throw new ApiException(401, ErrorCode.AUTH_DISABLED, "账号已被禁用");
        }
        return user;
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
    }

    private Map<String, Object> sessionResponse(User user) {
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

    private record Session(long userId, LocalDateTime expiresAt) {
    }
}

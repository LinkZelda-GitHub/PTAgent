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
import java.util.List;
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
        String realName = required(body, "realName", "真实姓名不能为空");
        String phoneNumber = required(body, "phoneNumber", "手机号不能为空");
        String education = required(body, "education", "学历不能为空");
        String email = Json.str(body, "email").toLowerCase();
        List<String> subjects = Json.stringList(body, "subjects");
        int gender = (int) Json.longValue(body, "gender", 1);

        if (!username.matches("[A-Za-z0-9_]{4,24}")) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "用户名需为4-24位字母、数字或下划线");
        }
        if (password.length() < 8 || password.length() > 64
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "密码需为8-64位，并同时包含字母和数字");
        }
        if (realName.length() < 2 || realName.length() > 30) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "真实姓名需为2-30个字符");
        }
        if (!phoneNumber.matches("1[3-9]\\d{9}")) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "请输入有效的11位手机号");
        }
        if (!email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "邮箱格式不正确");
        }
        if (subjects.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "请至少填写一个可授科目");
        }
        if (gender != 1 && gender != 2) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "性别参数不正确");
        }
        if (repository.findUserByUsername(username).isPresent()) {
            throw ApiException.badRequest(ErrorCode.AUTH_USERNAME_EXISTS, "用户名已存在");
        }
        if (repository.allUsers().stream().anyMatch(user -> phoneNumber.equals(user.phoneNumber))) {
            throw ApiException.badRequest(ErrorCode.AUTH_PHONE_EXISTS, "手机号已注册");
        }
        if (!email.isBlank() && repository.allUsers().stream().anyMatch(user -> email.equalsIgnoreCase(user.email))) {
            throw ApiException.badRequest(ErrorCode.AUTH_EMAIL_EXISTS, "邮箱已注册");
        }
        User user = repository.createUser(username, password, RoleType.TEACHER, phoneNumber, email, false);

        TeacherProfile profile = new TeacherProfile();
        profile.teacherId = user.id;
        profile.realName = realName;
        profile.gender = gender;
        profile.education = education;
        profile.graduateSchool = Json.str(body, "graduateSchool");
        profile.subjects = subjects;
        profile.serviceArea = Json.stringList(body, "serviceArea");
        profile.availableTime = Json.stringList(body, "availableTime");
        profile.teachingExperience = Json.str(body, "teachingExperience");
        profile.expectedRate = Json.str(body, "expectedRate");
        profile.contactPhone = phoneNumber;
        profile.contactWechat = Json.str(body, "contactWechat");
        profile.personalIntro = Json.str(body, "personalIntro");
        profile.hasTeacherCert = Json.bool(body, "hasTeacherCert", false);
        profile.is985 = Json.bool(body, "is985", false);
        profile.is211 = Json.bool(body, "is211", false);
        profile.isKeyUniversity = Json.bool(body, "isKeyUniversity", false);
        profile.normalUniversity = Json.bool(body, "normalUniversity", false);
        profile.competitionExperience = Json.bool(body, "competitionExperience", false);
        repository.saveProfile(profile);
        repository.createNotification(user.id, "账号待审核", "教师账号已注册，请等待最高管理员启用。");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("user", user.toPublicMap());
        response.put("profile", profile.toMap(user));
        response.put("status", "PENDING_REVIEW");
        response.put("message", "注册信息已提交，请等待最高管理员审核启用");
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

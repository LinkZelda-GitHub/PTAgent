package com.ptagent.service;

import com.ptagent.common.Json;
import com.ptagent.common.AppConfig;
import com.ptagent.domain.LoginMethod;
import com.ptagent.domain.RoleType;
import com.ptagent.domain.TeacherProfile;
import com.ptagent.domain.User;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;
import com.ptagent.repository.Repository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private static final int SESSION_HOURS = 12;
    private static final int PHONE_CODE_MINUTES = 5;
    private static final int PHONE_CODE_MAX_ATTEMPTS = 5;
    private static final int PHONE_SEND_COOLDOWN_SECONDS = 60;
    private static final int PHONE_SENDS_PER_HOUR = 5;
    private static final int LOGIN_FAILURE_LIMIT = 5;
    private static final int LOGIN_BLOCK_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Repository repository;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, PhoneCode> phoneCodes = new ConcurrentHashMap<>();
    private final Map<String, PhoneRequestWindow> phoneRequestWindows = new ConcurrentHashMap<>();
    private final Map<String, LoginFailure> loginFailures = new ConcurrentHashMap<>();

    public AuthService(Repository repository) {
        this.repository = repository;
    }

    public Map<String, Object> login(Map<String, Object> body) {
        LoginMethod loginMethod = loginMethod(body);
        String loginId = validatedLoginId(loginMethod, required(body, "loginId", "登录标识不能为空"));
        requireDemoProvider(loginMethod);
        String attemptKey = loginMethod.name() + ":" + loginId;
        requireLoginAllowed(attemptKey);
        User user;
        try {
            user = repository.findUserByLogin(loginMethod, loginId)
                    .orElseThrow(() -> ApiException.badRequest(ErrorCode.AUTH_USER_NOT_FOUND, "该登录方式尚未绑定账号"));
            if (!user.enabled) {
                throw ApiException.badRequest(ErrorCode.AUTH_DISABLED, "账号待审核或已被禁用");
            }
            if (loginMethod == LoginMethod.PHONE) {
                verifyPhoneCode(loginId, required(body, "verificationCode", "请输入手机验证码"));
            }
        } catch (ApiException exception) {
            recordLoginFailure(attemptKey);
            throw exception;
        }
        loginFailures.remove(attemptKey);

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

    public Map<String, Object> issuePhoneCode(String phoneNumber) {
        requireDemoProvider(LoginMethod.PHONE);
        String phone = validatedLoginId(LoginMethod.PHONE, phoneNumber);
        recordPhoneRequest(phone);
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(PHONE_CODE_MINUTES);
        phoneCodes.put(phone, new PhoneCode(code, expiresAt, 0));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("phoneNumber", maskPhone(phone));
        response.put("expiresInSeconds", PHONE_CODE_MINUTES * 60);
        response.put("demoCode", code);
        response.put("message", "验证码已生成");
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
            throw new ApiException(401, ErrorCode.AUTH_DISABLED, "账号待审核或已被禁用");
        }
        return user;
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
    }

    public Map<String, Object> registerTeacher(Map<String, Object> body) {
        LoginMethod loginMethod = loginMethod(body);
        String realName = required(body, "realName", "真实姓名不能为空");
        String phoneNumber = validatedLoginId(LoginMethod.PHONE,
                required(body, "phoneNumber", "手机号不能为空"));
        String rawLoginId = loginMethod == LoginMethod.PHONE ? phoneNumber
                : required(body, "loginId", "请填写要绑定的登录标识");
        String loginId = validatedLoginId(loginMethod, rawLoginId);
        String education = required(body, "education", "学历不能为空");
        String email = Json.str(body, "email").toLowerCase();
        List<String> subjects = Json.stringList(body, "subjects");
        int gender = (int) Json.longValue(body, "gender", 1);

        if (realName.length() < 2 || realName.length() > 30) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, "真实姓名需为2-30个字符");
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
        if (repository.findUserByLogin(loginMethod, loginId).isPresent()) {
            throw ApiException.badRequest(ErrorCode.AUTH_IDENTITY_EXISTS, "该登录标识已绑定账号");
        }
        if (repository.allUsers().stream().anyMatch(user -> phoneNumber.equals(user.phoneNumber))) {
            throw ApiException.badRequest(ErrorCode.AUTH_PHONE_EXISTS, "手机号已注册");
        }
        if (!email.isBlank() && repository.allUsers().stream().anyMatch(user -> email.equalsIgnoreCase(user.email))) {
            throw ApiException.badRequest(ErrorCode.AUTH_EMAIL_EXISTS, "邮箱已注册");
        }
        if (loginMethod == LoginMethod.PHONE) {
            verifyPhoneCode(phoneNumber, required(body, "verificationCode", "请输入手机验证码"));
        }

        User user = repository.createUser(realName, loginMethod, loginId, RoleType.TEACHER, phoneNumber, email, false);
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

    private Map<String, Object> sessionResponse(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("user", user.toPublicMap());
        repository.findProfile(user.id).ifPresent(profile -> response.put("profile", profile.toMap(user)));
        return response;
    }

    private LoginMethod loginMethod(Map<String, Object> body) {
        String value = Json.str(body, "loginMethod").toUpperCase();
        try {
            return LoginMethod.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw ApiException.badRequest(ErrorCode.AUTH_LOGIN_METHOD_INVALID, "请选择微信、QQ或手机号登录");
        }
    }

    private String validatedLoginId(LoginMethod loginMethod, String value) {
        String loginId = loginMethod.normalize(value);
        boolean valid = switch (loginMethod) {
            case WECHAT -> loginId.matches("[a-z][a-z0-9_-]{3,63}");
            case QQ -> loginId.matches("[1-9]\\d{4,11}");
            case PHONE -> loginId.matches("1[3-9]\\d{9}");
        };
        if (!valid) {
            String message = switch (loginMethod) {
                case WECHAT -> "请输入有效的微信标识";
                case QQ -> "请输入有效的QQ号";
                case PHONE -> "请输入有效的11位手机号";
            };
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, message);
        }
        return loginId;
    }

    private void verifyPhoneCode(String phoneNumber, String submittedCode) {
        PhoneCode phoneCode = phoneCodes.get(phoneNumber);
        if (phoneCode == null) {
            throw ApiException.badRequest(ErrorCode.AUTH_VERIFICATION_CODE_INVALID, "请先获取手机验证码");
        }
        if (phoneCode.expiresAt().isBefore(LocalDateTime.now())) {
            phoneCodes.remove(phoneNumber);
            throw ApiException.badRequest(ErrorCode.AUTH_VERIFICATION_CODE_EXPIRED, "手机验证码已过期");
        }
        if (!phoneCode.code().equals(submittedCode.trim())) {
            int attempts = phoneCode.attempts() + 1;
            if (attempts >= PHONE_CODE_MAX_ATTEMPTS) {
                phoneCodes.remove(phoneNumber);
                throw new ApiException(429, ErrorCode.AUTH_RATE_LIMITED, "验证码错误次数过多，请重新获取");
            }
            phoneCodes.put(phoneNumber, new PhoneCode(phoneCode.code(), phoneCode.expiresAt(), attempts));
            throw ApiException.badRequest(ErrorCode.AUTH_VERIFICATION_CODE_INVALID,
                    "手机验证码不正确，还可尝试" + (PHONE_CODE_MAX_ATTEMPTS - attempts) + "次");
        }
        phoneCodes.remove(phoneNumber);
    }

    private void requireDemoProvider(LoginMethod loginMethod) {
        if (!AppConfig.demoAuthEnabled()) {
            throw new ApiException(503, ErrorCode.AUTH_PROVIDER_UNAVAILABLE,
                    loginMethod.label + "认证服务尚未接入");
        }
    }

    private void recordPhoneRequest(String phone) {
        LocalDateTime now = LocalDateTime.now();
        PhoneRequestWindow current = phoneRequestWindows.get(phone);
        if (current != null && current.lastSentAt().plusSeconds(PHONE_SEND_COOLDOWN_SECONDS).isAfter(now)) {
            throw new ApiException(429, ErrorCode.AUTH_RATE_LIMITED, "验证码发送过于频繁，请稍后再试");
        }
        if (current == null || current.windowStartedAt().plusHours(1).isBefore(now)) {
            phoneRequestWindows.put(phone, new PhoneRequestWindow(now, now, 1));
            return;
        }
        if (current.sentCount() >= PHONE_SENDS_PER_HOUR) {
            throw new ApiException(429, ErrorCode.AUTH_RATE_LIMITED, "验证码发送次数已达上限");
        }
        phoneRequestWindows.put(phone,
                new PhoneRequestWindow(current.windowStartedAt(), now, current.sentCount() + 1));
    }

    private void requireLoginAllowed(String attemptKey) {
        LoginFailure failure = loginFailures.get(attemptKey);
        if (failure == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (failure.blockedUntil() != null && failure.blockedUntil().isAfter(now)) {
            throw new ApiException(429, ErrorCode.AUTH_RATE_LIMITED, "登录尝试次数过多，请稍后再试");
        }
        if (failure.firstFailureAt().plusMinutes(LOGIN_BLOCK_MINUTES).isBefore(now)) {
            loginFailures.remove(attemptKey);
        }
    }

    private void recordLoginFailure(String attemptKey) {
        LocalDateTime now = LocalDateTime.now();
        LoginFailure current = loginFailures.get(attemptKey);
        if (current == null || current.firstFailureAt().plusMinutes(LOGIN_BLOCK_MINUTES).isBefore(now)) {
            loginFailures.put(attemptKey, new LoginFailure(1, now, null));
            return;
        }
        int failures = current.failures() + 1;
        LocalDateTime blockedUntil = failures >= LOGIN_FAILURE_LIMIT
                ? now.plusMinutes(LOGIN_BLOCK_MINUTES) : null;
        loginFailures.put(attemptKey, new LoginFailure(failures, current.firstFailureAt(), blockedUntil));
    }

    private String required(Map<String, Object> body, String key, String message) {
        String value = Json.str(body, key);
        if (value.isBlank()) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_ERROR, message);
        }
        return value;
    }

    private String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private record Session(long userId, LocalDateTime expiresAt) {
    }

    private record PhoneCode(String code, LocalDateTime expiresAt, int attempts) {
    }

    private record PhoneRequestWindow(LocalDateTime windowStartedAt, LocalDateTime lastSentAt, int sentCount) {
    }

    private record LoginFailure(int failures, LocalDateTime firstFailureAt, LocalDateTime blockedUntil) {
    }
}

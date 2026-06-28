package com.ptagent.repository;

import com.ptagent.common.Json;
import com.ptagent.domain.LoginMethod;
import com.ptagent.domain.RoleType;
import com.ptagent.domain.TeacherProfile;
import com.ptagent.domain.User;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AccountDatabase {
    static final String FILE_NAME = "ptagent-accounts.json";

    private final Path file;

    AccountDatabase(Path dataDirectory) {
        this.file = dataDirectory.toAbsolutePath().normalize().resolve(FILE_NAME);
    }

    static Path defaultDirectory() {
        String configured = System.getenv("PTAGENT_DATA_DIR");
        return configured == null || configured.isBlank() ? Path.of("data") : Path.of(configured.trim());
    }

    synchronized Snapshot read() {
        if (!Files.exists(file)) {
            return new Snapshot(List.of(), List.of());
        }
        try {
            Map<String, Object> root = Json.parseObject(Files.readString(file, StandardCharsets.UTF_8));
            return new Snapshot(readUsers(root.get("users")), readProfiles(root.get("profiles")));
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("无法读取账号数据库：" + file, exception);
        }
    }

    synchronized void write(List<User> users, List<TeacherProfile> profiles) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", 2);
        root.put("updatedAt", LocalDateTime.now().toString());
        root.put("users", users.stream().map(this::userMap).toList());
        root.put("profiles", profiles.stream().map(this::profileMap).toList());

        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temporary, Json.stringify(root), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("无法写入账号数据库：" + file, exception);
        }
    }

    String location() {
        return file.toString();
    }

    boolean healthy() {
        Path directory = file.getParent();
        return (Files.notExists(file) || Files.isReadable(file))
                && (Files.notExists(directory) || Files.isWritable(directory));
    }

    private Map<String, Object> userMap(User user) {
        return Json.object(
                "id", user.id, "displayName", user.displayName,
                "loginMethod", user.loginMethod.name(), "loginId", user.loginId,
                "role", user.role.name(), "phoneNumber", user.phoneNumber, "email", user.email,
                "enabled", user.enabled, "registerTime", text(user.registerTime), "lastLogin", text(user.lastLogin)
        );
    }

    private Map<String, Object> profileMap(TeacherProfile profile) {
        return Json.object(
                "teacherId", profile.teacherId, "realName", profile.realName, "gender", profile.gender,
                "education", profile.education, "graduateSchool", profile.graduateSchool,
                "is985", profile.is985, "is211", profile.is211,
                "isKeyUniversity", profile.isKeyUniversity, "subjects", profile.subjects,
                "teachingExperience", profile.teachingExperience, "expectedRate", profile.expectedRate,
                "availableTime", profile.availableTime, "serviceArea", profile.serviceArea,
                "personalIntro", profile.personalIntro, "avgRating", profile.avgRating,
                "contactPhone", profile.contactPhone, "contactWechat", profile.contactWechat,
                "hasTeacherCert", profile.hasTeacherCert, "normalUniversity", profile.normalUniversity,
                "competitionExperience", profile.competitionExperience
        );
    }

    private List<User> readUsers(Object value) {
        List<User> result = new ArrayList<>();
        for (Map<String, Object> item : objectList(value)) {
            User user = new User();
            user.id = Json.longValue(item, "id", 0);
            user.role = RoleType.valueOf(Json.str(item, "role"));
            user.phoneNumber = Json.str(item, "phoneNumber");
            user.email = Json.str(item, "email");
            String legacyUsername = Json.str(item, "username");
            user.displayName = Json.str(item, "displayName");
            if (user.displayName.isBlank()) {
                user.displayName = legacyUsername.isBlank() ? user.role.label : legacyUsername;
            }
            String method = Json.str(item, "loginMethod");
            user.loginMethod = method.isBlank() ? legacyLoginMethod(legacyUsername)
                    : LoginMethod.valueOf(method);
            user.loginId = user.loginMethod.normalize(Json.str(item, "loginId"));
            if (user.loginId.isBlank()) {
                user.loginId = legacyLoginId(legacyUsername, user.phoneNumber, user.loginMethod);
            }
            user.enabled = Json.bool(item, "enabled", false);
            user.registerTime = dateTime(item, "registerTime");
            user.lastLogin = dateTime(item, "lastLogin");
            if (user.id > 0 && !user.displayName.isBlank() && !user.loginId.isBlank()) {
                result.add(user);
            }
        }
        return result;
    }

    private List<TeacherProfile> readProfiles(Object value) {
        List<TeacherProfile> result = new ArrayList<>();
        for (Map<String, Object> item : objectList(value)) {
            TeacherProfile profile = new TeacherProfile();
            profile.teacherId = Json.longValue(item, "teacherId", 0);
            profile.realName = Json.str(item, "realName");
            profile.gender = (int) Json.longValue(item, "gender", 1);
            profile.education = Json.str(item, "education");
            profile.graduateSchool = Json.str(item, "graduateSchool");
            profile.is985 = Json.bool(item, "is985", false);
            profile.is211 = Json.bool(item, "is211", false);
            profile.isKeyUniversity = Json.bool(item, "isKeyUniversity", false);
            profile.subjects = Json.stringList(item, "subjects");
            profile.teachingExperience = Json.str(item, "teachingExperience");
            profile.expectedRate = Json.str(item, "expectedRate");
            profile.availableTime = Json.stringList(item, "availableTime");
            profile.serviceArea = Json.stringList(item, "serviceArea");
            profile.personalIntro = Json.str(item, "personalIntro");
            profile.avgRating = Json.doubleValue(item, "avgRating", 5.0);
            profile.contactPhone = Json.str(item, "contactPhone");
            profile.contactWechat = Json.str(item, "contactWechat");
            profile.hasTeacherCert = Json.bool(item, "hasTeacherCert", false);
            profile.normalUniversity = Json.bool(item, "normalUniversity", false);
            profile.competitionExperience = Json.bool(item, "competitionExperience", false);
            if (profile.teacherId > 0) {
                result.add(profile);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> objectList(Object value) {
        if (!(value instanceof List<?> items)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private LocalDateTime dateTime(Map<String, Object> item, String key) {
        String value = Json.str(item, key);
        return value.isBlank() ? null : LocalDateTime.parse(value);
    }

    private LoginMethod legacyLoginMethod(String username) {
        if ("super".equalsIgnoreCase(username) || "wang".equalsIgnoreCase(username)) {
            return LoginMethod.WECHAT;
        }
        if ("admin".equalsIgnoreCase(username)) {
            return LoginMethod.QQ;
        }
        return LoginMethod.PHONE;
    }

    private String legacyLoginId(String username, String phoneNumber, LoginMethod loginMethod) {
        if (loginMethod == LoginMethod.WECHAT) {
            return "super".equalsIgnoreCase(username) ? "ptagent_super" : "wangfang_edu";
        }
        if (loginMethod == LoginMethod.QQ) {
            return "10001001";
        }
        return phoneNumber;
    }

    private String text(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    record Snapshot(List<User> users, List<TeacherProfile> profiles) {
    }
}

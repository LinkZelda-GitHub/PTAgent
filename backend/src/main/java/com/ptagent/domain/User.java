package com.ptagent.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class User {
    public long id;
    public String displayName;
    public LoginMethod loginMethod;
    public String loginId;
    public RoleType role;
    public String phoneNumber;
    public String email;
    public boolean enabled = true;
    public LocalDateTime registerTime = LocalDateTime.now();
    public LocalDateTime lastLogin;

    public Map<String, Object> toPublicMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("displayName", displayName);
        map.put("loginMethod", loginMethod.name());
        map.put("loginMethodLabel", loginMethod.label);
        map.put("maskedLoginId", maskedLoginId());
        map.put("role", role.name());
        map.put("roleLabel", role.label);
        map.put("phoneNumber", phoneNumber);
        map.put("email", email);
        map.put("enabled", enabled);
        map.put("registerTime", registerTime == null ? null : registerTime.toString());
        map.put("lastLogin", lastLogin == null ? null : lastLogin.toString());
        return map;
    }

    private String maskedLoginId() {
        if (loginId == null || loginId.isBlank()) {
            return "";
        }
        if (loginMethod == LoginMethod.PHONE && loginId.length() == 11) {
            return loginId.substring(0, 3) + "****" + loginId.substring(7);
        }
        if (loginId.length() <= 4) {
            return loginId.charAt(0) + "***";
        }
        return loginId.substring(0, 2) + "***" + loginId.substring(loginId.length() - 2);
    }
}

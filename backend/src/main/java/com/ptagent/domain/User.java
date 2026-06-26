package com.ptagent.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class User {
    public long id;
    public String username;
    public String passwordHash;
    public RoleType role;
    public String phoneNumber;
    public String email;
    public boolean enabled = true;
    public LocalDateTime registerTime = LocalDateTime.now();
    public LocalDateTime lastLogin;

    public Map<String, Object> toPublicMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("username", username);
        map.put("role", role.name());
        map.put("roleLabel", role.label);
        map.put("phoneNumber", phoneNumber);
        map.put("email", email);
        map.put("enabled", enabled);
        map.put("registerTime", registerTime == null ? null : registerTime.toString());
        map.put("lastLogin", lastLogin == null ? null : lastLogin.toString());
        return map;
    }
}

package com.ptagent.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuditLog {
    public long id;
    public long actorId;
    public String actorRole;
    public String action;
    public String targetType;
    public long targetId;
    public String detail;
    public String requestId = "";
    public String clientIp = "";
    public String userAgent = "";
    public String result = "SUCCESS";
    public String previousHash = "";
    public String hash = "";
    public LocalDateTime createTime = LocalDateTime.now();

    public Map<String, Object> contentMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("actorId", actorId);
        map.put("actorRole", actorRole);
        map.put("action", action);
        map.put("targetType", targetType);
        map.put("targetId", targetId);
        map.put("detail", detail);
        map.put("requestId", requestId);
        map.put("clientIp", clientIp);
        map.put("userAgent", userAgent);
        map.put("result", result);
        map.put("createTime", createTime == null ? null : createTime.toString());
        return map;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = contentMap();
        map.put("previousHash", previousHash);
        map.put("hash", hash);
        return map;
    }
}

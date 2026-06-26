package com.ptagent.domain;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class Notification {
    public long id;
    public long userId;
    public String title;
    public String message;
    public LocalDateTime createTime = LocalDateTime.now();
    public boolean read;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("userId", userId);
        map.put("title", title);
        map.put("message", message);
        map.put("createTime", createTime == null ? null : createTime.toString());
        map.put("read", read);
        return map;
    }
}

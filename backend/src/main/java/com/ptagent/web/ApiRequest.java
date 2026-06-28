package com.ptagent.web;

import com.ptagent.domain.RoleType;
import com.ptagent.domain.User;
import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ApiRequest(String method, List<String> path, Map<String, String> query, Map<String, Object> body,
                         String bearerToken, User authenticatedUser) {
    public boolean method(String expected) {
        return method.equalsIgnoreCase(expected);
    }

    public boolean is(String... expected) {
        if (path.size() != expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (!expected[i].equals(path.get(i))) {
                return false;
            }
        }
        return true;
    }

    public long pathId(int index) {
        try {
            return Long.parseLong(path.get(index));
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw ApiException.badRequest(ErrorCode.INVALID_ID, "ID格式不正确");
        }
    }

    public long queryLong(String key, long fallback) {
        String value = query.get(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public ApiRequest authenticated(User user) {
        return new ApiRequest(method, path, query, body, bearerToken, user);
    }

    public User actor() {
        if (authenticatedUser == null) {
            throw new ApiException(401, ErrorCode.AUTH_REQUIRED, "请先登录");
        }
        return authenticatedUser;
    }

    public long actorId() {
        return actor().id;
    }

    public boolean actorIs(RoleType role) {
        return actor().role == role;
    }

    public Map<String, Object> bodyWithActor(String actorKey) {
        Map<String, Object> trustedBody = new LinkedHashMap<>(body);
        trustedBody.put(actorKey, actorId());
        return trustedBody;
    }

    public Map<String, String> queryForActorRole(String identityKey, RoleType role) {
        Map<String, String> trustedQuery = new LinkedHashMap<>(query);
        trustedQuery.remove(identityKey);
        if (actorIs(role)) {
            trustedQuery.put(identityKey, String.valueOf(actorId()));
        }
        return trustedQuery;
    }
}

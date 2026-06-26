package com.ptagent.web;

import com.ptagent.exception.ApiException;
import com.ptagent.exception.ErrorCode;

import java.util.List;
import java.util.Map;

public record ApiRequest(String method, List<String> path, Map<String, String> query, Map<String, Object> body) {
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
}

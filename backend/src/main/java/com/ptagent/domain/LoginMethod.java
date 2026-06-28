package com.ptagent.domain;

import java.util.Locale;

public enum LoginMethod {
    WECHAT("微信"),
    QQ("QQ"),
    PHONE("手机号");

    public final String label;

    LoginMethod(String label) {
        this.label = label;
    }

    public String normalize(String value) {
        String normalized = value == null ? "" : value.trim();
        return this == WECHAT ? normalized.toLowerCase(Locale.ROOT) : normalized;
    }
}

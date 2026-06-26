package com.ptagent.domain;

public enum RoleType {
    SUPER_ADMIN(1, "最高管理员"),
    ADMIN(2, "普通管理员"),
    TEACHER(3, "教师");

    public final int code;
    public final String label;

    RoleType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}

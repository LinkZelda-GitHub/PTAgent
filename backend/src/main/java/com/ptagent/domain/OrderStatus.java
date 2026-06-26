package com.ptagent.domain;

public enum OrderStatus {
    WAITING(1, "待授课"),
    TEACHING(2, "授课中"),
    FINISHED(3, "已完成"),
    CANCELLED(4, "已取消");

    public final int code;
    public final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}

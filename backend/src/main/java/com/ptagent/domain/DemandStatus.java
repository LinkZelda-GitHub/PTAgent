package com.ptagent.domain;

public enum DemandStatus {
    OPEN(0, "待接单"),
    MATCHING(1, "匹配中"),
    TEACHING(2, "授课中"),
    FINISHED(3, "已完成"),
    CLOSED(4, "已关闭");

    public final int code;
    public final String label;

    DemandStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}

package com.ptagent.domain;

public enum ApplicationStatus {
    PENDING(0, "待审核"),
    APPROVED(1, "通过"),
    REJECTED(2, "未通过");

    public final int code;
    public final String label;

    ApplicationStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }
}

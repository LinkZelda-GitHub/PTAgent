package com.ptagent.exception;

public class ApiException extends IllegalArgumentException {
    private final int status;
    private final ErrorCode code;

    public ApiException(int status, ErrorCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int status() {
        return status;
    }

    public ErrorCode code() {
        return code;
    }

    public static ApiException badRequest(ErrorCode code, String message) {
        return new ApiException(400, code, message);
    }

    public static ApiException notFound(ErrorCode code, String message) {
        return new ApiException(404, code, message);
    }
}

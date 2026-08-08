package com.web.backend.exception.custom;

public class SystemOverloadException extends RuntimeException {
    private Object requestData;

    public SystemOverloadException(String message) {
        super(message);
    }

    public SystemOverloadException(String message, Object requestData) {
        super(message);
        this.requestData = requestData;
    }

    public SystemOverloadException(String message, Object requestData, Throwable cause) {
        super(message, cause);
        this.requestData = requestData;
    }

    public Object getRequestData() {
        return requestData;
    }
}

package com.web.backend.exception.custom;

public class AccessForbiddenException extends RuntimeException {
    private Object requestData;

    public AccessForbiddenException(String message) {
        super(message);
    }

    public AccessForbiddenException(String message, Object requestData) {
        super(message);
        this.requestData = requestData;
    }

    public Object getRequestData() {
        return requestData;
    }
}
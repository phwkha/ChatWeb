package com.web.backend.exception.custom;

public class ResourceConflictException extends RuntimeException {
    private Object requestData;

    public ResourceConflictException(String message) {
        super(message);
    }

    public ResourceConflictException(String message, Object requestData) {
        super(message);
        this.requestData = requestData;
    }

    public Object getRequestData() {
        return requestData;
    }
}
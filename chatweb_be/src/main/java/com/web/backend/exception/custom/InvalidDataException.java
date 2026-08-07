package com.web.backend.exception.custom;

public class InvalidDataException extends RuntimeException {
    private Object requestData;

    public InvalidDataException(String message) {
        super(message);
    }

    public InvalidDataException(String message, Object requestData) {
        super(message);
        this.requestData = requestData;
    }

    public Object getRequestData() {
        return requestData;
    }
}

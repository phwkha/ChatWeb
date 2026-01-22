package com.web.backend.controller.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorDebugInfo {
    private String exceptionType;
    private String devMessage;
    private String stackTrace;
    private String path;
}
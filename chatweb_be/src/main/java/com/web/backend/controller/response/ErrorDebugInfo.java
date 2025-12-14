package com.web.backend.controller.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorDebugInfo {
    private String exceptionType; // Tên lỗi (NullPointer, IllegalArgument...)
    private String devMessage;    // Message gốc của hệ thống (tiếng Anh)
    private String stackTrace;    // (Tùy chọn) Dòng lỗi chi tiết
    private String path;          // API nào bị lỗi
}
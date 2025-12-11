package com.web.backend.exception;

import com.web.backend.controller.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleHttpRequestMethodNotSupportedException(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        return ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "Phương thức " + ex.getMethod() + " không được hỗ trợ cho endpoint này");
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingServletRequestParameterException(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Thiếu tham số bắt buộc: " + ex.getParameterName());
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        String message = String.format("Tham số '%s' không đúng định dạng mong muốn", ex.getName());
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON Request: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Định dạng dữ liệu gửi lên không đúng (JSON lỗi)");
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDataIntegrityViolationException(org.springframework.dao.DataIntegrityViolationException ex) {
        log.error("Database constraint violation: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.CONFLICT.value(), "Dữ liệu bị trùng lặp hoặc vi phạm ràng buộc cơ sở dữ liệu");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleSpringAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        return ApiResponse.error(HttpStatus.FORBIDDEN.value(), "Bạn không có quyền truy cập tài nguyên này (Role Denied)");
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleAuthenticationFailedException(AuthenticationFailedException ex) {
        log.error("Authentication Failed: {}" ,ex.getMessage());
        return ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
    }

    @ExceptionHandler(PasswordMismatchException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handlePasswordMismatchException(PasswordMismatchException ex) {
        log.error("Password Mismatch: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleInvalidPasswordException(InvalidPasswordException ex) {
        log.error("Invalid Password: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
    }

    @ExceptionHandler(InvalidOtpException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidOtpException(InvalidOtpException ex) {
        log.warn("OTP Validation Failed: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(InvalidDataException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleInvalidDataException(InvalidOtpException ex) {
        log.warn("Invalid Data Failed: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource Not Found: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(ResourceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleResourceConflictException(ResourceConflictException ex) {
        log.warn("Resource Conflict: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(AccessForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessForbiddenException(AccessForbiddenException ex) {
        log.warn("Access Forbidden: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.FORBIDDEN.value(), ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        log.error("Generic Logic Error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    // Bắt lỗi Validate (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.success(HttpStatus.BAD_REQUEST.value(), "Dữ liệu đầu vào không hợp lệ",errors));
    }

    // Bắt lỗi hệ thống khác
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("System Error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Lỗi hệ thống: " + ex.getMessage()));
    }
}
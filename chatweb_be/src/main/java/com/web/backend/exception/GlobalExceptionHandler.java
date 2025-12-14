package com.web.backend.exception;

import com.web.backend.controller.response.ApiResponse;
import com.web.backend.controller.response.ErrorDebugInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@Slf4j(topic = "GLOBAL-EXCEPTION-HANDLER")
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleDisabledException(DisabledException ex) {
        log.error("Account Disabled");
        return ApiResponse.error(HttpStatus.FORBIDDEN.value(), ex.getMessage());
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleLockedException(LockedException ex) {
        log.error("Account Locked");
        return ApiResponse.error(HttpStatus.FORBIDDEN.value(), ex.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ApiResponse<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        log.error("Http Request Method Not Supported: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.METHOD_NOT_ALLOWED.value(), "Phương thức " + ex.getMethod() + " không được hỗ trợ cho endpoint này");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        log.error("Missing Servlet Request Parameter: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Thiếu tham số bắt buộc: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.error("Method Argument Type Mismatch: {}", ex.getMessage());
        String message = String.format("Tham số '%s' không đúng định dạng mong muốn", ex.getName());
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Bad JSON: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Dữ liệu gửi lên không đúng định dạng.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("DB Constraint: {}", ex.getMessage());
        return ApiResponse.error(HttpStatus.CONFLICT.value(), "Dữ liệu không hợp lệ hoặc đã tồn tại trong hệ thống.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleSpringAccessDeniedException(AccessDeniedException ex) {
        log.error("Spring Access Denied: {}" ,ex.getMessage());
        return ApiResponse.error(HttpStatus.FORBIDDEN.value(), ex.getMessage());
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
    public ApiResponse<Void> handleInvalidDataException(InvalidDataException ex) {
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
        log.error("Logic Error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Đã xảy ra lỗi xử lý, vui lòng thử lại."));
    }

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("System Error (Uncaught): ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "Hệ thống đang bận, vui lòng thử lại sau."));
    }

    private ApiResponse<ErrorDebugInfo> buildErrorForDev(HttpStatus status, Exception ex, String userMessage) {

        ErrorDebugInfo debugInfo = ErrorDebugInfo.builder()
                .exceptionType(ex.getClass().getSimpleName())
                .devMessage(ex.getMessage())
                .build();

        return ApiResponse.<ErrorDebugInfo>builder()
                .code(status.value())
                .status("error")
                .message(userMessage)
                .data(debugInfo)
                .build();
    }
}
package com.web.backend.exception;

import com.web.backend.config.localresolverconfig.Translator;
import com.web.backend.controller.response.form.SocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.validation.BindingResult;

@ControllerAdvice
@RequiredArgsConstructor
public class WebSocketErrorHandler {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final String QUEUE_ERRORS_STRING = "/queue/errors";

    private static final String ERROR_WS_INVALID_DATA_STRING = "error.ws.invalid_data";
    private static final String ERROR_SYS_BAD_FORMAT_STRING = "error.sys.bad_format";
    private static final String ERROR_SYS_BUSY_STRING = "error.sys.busy";

    public void handleChatError(String username, Object request, String message) {
        simpMessagingTemplate.convertAndSendToUser(
                username,
                QUEUE_ERRORS_STRING,
                SocketResponse.error(message, request));
    }

    public void handleChatError(Authentication authentication, String sessionId, Object request, String message) {
        if (authentication != null && authentication.getName() != null) {
            handleChatError(authentication.getName(), request, message);
        } else if (sessionId != null) {
            org.springframework.messaging.simp.SimpMessageHeaderAccessor headerAccessor = org.springframework.messaging.simp.SimpMessageHeaderAccessor
                    .create(org.springframework.messaging.simp.SimpMessageType.MESSAGE);
            headerAccessor.setSessionId(sessionId);
            headerAccessor.setLeaveMutable(true);
            simpMessagingTemplate.convertAndSendToUser(
                    sessionId,
                    QUEUE_ERRORS_STRING,
                    SocketResponse.error(message, request),
                    headerAccessor.getMessageHeaders());
        }
    }

    @MessageExceptionHandler(org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException.class)
    public void handleWebSocketValidationException(
            org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException ex,
            Authentication authentication,
            @org.springframework.messaging.handler.annotation.Header(value = "simpSessionId", required = false) String sessionId) {

        String errorMessage = Translator.tolocale(ERROR_WS_INVALID_DATA_STRING);
        Object requestData = null;

        BindingResult bindingResult = ex.getBindingResult();
        if (bindingResult != null) {
            requestData = bindingResult.getTarget();
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                errorMessage = fieldError.getDefaultMessage();
            }
        }

        this.handleChatError(authentication, sessionId, requestData, errorMessage);
    }

    @MessageExceptionHandler(MessageConversionException.class)
    public void handleMessageConversionException(
            MessageConversionException ex,
            Authentication authentication,
            @org.springframework.messaging.handler.annotation.Header(value = "simpSessionId", required = false) String sessionId) {

        String errorMessage = Translator.tolocale(ERROR_SYS_BAD_FORMAT_STRING);

        this.handleChatError(authentication, sessionId, null, errorMessage);
    }

    @MessageExceptionHandler(com.web.backend.exception.custom.AccessForbiddenException.class)
    public void handleAccessForbiddenException(
            com.web.backend.exception.custom.AccessForbiddenException ex,
            Authentication authentication,
            @org.springframework.messaging.handler.annotation.Header(value = "simpSessionId", required = false) String sessionId) {
        this.handleChatError(authentication, sessionId, ex.getRequestData(), ex.getMessage());
    }

    @MessageExceptionHandler(com.web.backend.exception.custom.InvalidDataException.class)
    public void handleInvalidDataException(
            com.web.backend.exception.custom.InvalidDataException ex,
            Authentication authentication,
            @org.springframework.messaging.handler.annotation.Header(value = "simpSessionId", required = false) String sessionId) {
        this.handleChatError(authentication, sessionId, ex.getRequestData(), ex.getMessage());
    }

    @MessageExceptionHandler(com.web.backend.exception.custom.ResourceNotFoundException.class)
    public void handleResourceNotFoundException(
            com.web.backend.exception.custom.ResourceNotFoundException ex,
            Authentication authentication,
            @org.springframework.messaging.handler.annotation.Header(value = "simpSessionId", required = false) String sessionId) {
        this.handleChatError(authentication, sessionId, ex.getRequestData(), ex.getMessage());
    }

    @MessageExceptionHandler(com.web.backend.exception.custom.ResourceConflictException.class)
    public void handleResourceConflictException(
            com.web.backend.exception.custom.ResourceConflictException ex,
            Authentication authentication,
            @org.springframework.messaging.handler.annotation.Header(value = "simpSessionId", required = false) String sessionId) {
        this.handleChatError(authentication, sessionId, ex.getRequestData(), ex.getMessage());
    }

    @MessageExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public void handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex,
            Authentication authentication,
            @org.springframework.messaging.handler.annotation.Header(value = "simpSessionId", required = false) String sessionId) {

        this.handleChatError(authentication, sessionId, null, ex.getMessage());
    }

    @MessageExceptionHandler(Exception.class)
    public void handleAllOtherExceptions(
            Exception ex,
            Authentication authentication,
            @org.springframework.messaging.handler.annotation.Header(value = "simpSessionId", required = false) String sessionId) {

        String errorMessage = Translator.tolocale(ERROR_SYS_BUSY_STRING);

        this.handleChatError(authentication, sessionId, null, errorMessage);
    }
}

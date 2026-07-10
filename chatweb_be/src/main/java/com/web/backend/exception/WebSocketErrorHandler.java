package com.web.backend.exception;

import com.web.backend.controller.response.form.SocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.validation.BindingResult;

@ControllerAdvice
@RequiredArgsConstructor
public class WebSocketErrorHandler {

    private final SimpMessagingTemplate simpMessagingTemplate;

    public void handleChatError(String username, Object request, String message) {
        String targetUser = (username == null || username.trim().isEmpty()) ? "unknows" : username;

        simpMessagingTemplate.convertAndSendToUser(
                targetUser,
                "/queue/errors",
                SocketResponse.error(message, request));
    }

    @MessageExceptionHandler(org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException.class)
    public void handleWebSocketValidationException(
            org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException ex,
            Authentication authentication) {

        String username = (authentication != null) ? authentication.getName() : "unknows";

        String errorMessage = "Dữ liệu gửi lên không hợp lệ";

        BindingResult bindingResult = ex.getBindingResult();
        if (bindingResult != null) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                errorMessage = fieldError.getDefaultMessage();
            }
        }

        this.handleChatError(username, null, errorMessage);
    }
}

package com.web.backend.exception;

import com.web.backend.controller.response.form.SocketResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.security.core.Authentication;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.validation.BindingResult;
import com.web.backend.config.LocalResolverConfig.Translator;

@ControllerAdvice
@RequiredArgsConstructor
public class WebSocketErrorHandler {

    private final SimpMessagingTemplate simpMessagingTemplate;

    private static final String QUEUE_ERRORS_STRING = "/queue/errors";

    private static final String UNKNOWS_STRING = "unknows";

    private static final String ERROR_WS_INVALID_DATA_STRING = "error.ws.invalid_data";
    private static final String ERROR_SYS_BAD_FORMAT_STRING = "error.sys.bad_format";

    public void handleChatError(String username, Object request, String message) {
        String targetUser = (username == null || username.trim().isEmpty()) ? UNKNOWS_STRING : username;

        simpMessagingTemplate.convertAndSendToUser(
                targetUser,
                QUEUE_ERRORS_STRING,
                SocketResponse.error(message, request));
    }

    @MessageExceptionHandler(org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException.class)
    public void handleWebSocketValidationException(
            org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException ex,
            Authentication authentication) {

        String username = (authentication != null) ? authentication.getName() : UNKNOWS_STRING;

        String errorMessage = Translator.tolocale(ERROR_WS_INVALID_DATA_STRING);

        BindingResult bindingResult = ex.getBindingResult();
        if (bindingResult != null) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                errorMessage = fieldError.getDefaultMessage();
            }
        }

        this.handleChatError(username, null, errorMessage);
    }

    @MessageExceptionHandler(MessageConversionException.class)
    public void handleMessageConversionException(
            MessageConversionException ex,
            Authentication authentication) {

        String username = (authentication != null) ? authentication.getName() : UNKNOWS_STRING;

        String errorMessage = Translator.tolocale(ERROR_SYS_BAD_FORMAT_STRING);

        this.handleChatError(username, null, errorMessage);
    }
}

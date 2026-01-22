package com.web.backend.listener;

import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.form.SocketResponse;
import com.web.backend.event.NewChatMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventListener {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Async
    @EventListener
    public void handleNewChatMessage(NewChatMessageEvent event) {
        ChatMessageResponse messageResponse = event.getResponse();

        log.info("Sending new message notification from {} to {}", event.getSenderUsername(), event.getRecipientUsername());

        SocketResponse<ChatMessageResponse> socketResponse = SocketResponse.message(messageResponse);
        simpMessagingTemplate.convertAndSendToUser(
                event.getRecipientUsername(),
                "/queue/private",
                socketResponse
        );

        simpMessagingTemplate.convertAndSendToUser(
                event.getSenderUsername(),
                "/queue/private",
                socketResponse
        );
    }
}
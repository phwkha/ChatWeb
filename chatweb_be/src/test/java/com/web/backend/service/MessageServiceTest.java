package com.web.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.web.backend.common.UserStatus;
import com.web.backend.config.LocalResolverConfig.Translator;
import com.web.backend.controller.request.ChatMessageRequest;
import com.web.backend.controller.request.EditMessageRequest;
import com.web.backend.controller.request.RevokeMessageRequest;
import com.web.backend.exception.WebSocketErrorHandler;
import com.web.backend.exception.custom.AccessForbiddenException;
import com.web.backend.exception.custom.ResourceNotFoundException;
import com.web.backend.mapper.MessageMapper;
import com.web.backend.model.ChatMessage;
import com.web.backend.model.SystemMessage;
import com.web.backend.model.UserEntity;
import com.web.backend.repository.MessageRepository;
import com.web.backend.repository.SystemMessageRepository;
import com.web.backend.repository.UserRepository;
import com.web.backend.repository.projection.UnreadCountProjection;
import com.web.backend.service.impl.MessageServiceImpl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import org.springframework.data.domain.Pageable;
import com.web.backend.common.MessageStatus;
import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.controller.response.CursorResponse;
import com.web.backend.controller.response.MessageSystemResponse;
import com.web.backend.controller.response.UnreadCountsResponse;

@ExtendWith(MockitoExtension.class)
public class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SystemMessageRepository systemMessageRepository;
    @Mock
    private FriendService friendService;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private MessageMapper messageMapper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private WebSocketErrorHandler webSocketErrorHandler;

    @Mock
    private ListOperations<String, Object> listOperations;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private MessageServiceImpl messageService;

    private UserEntity recipientUser;

    @BeforeEach
    void setUp() {
        // Mock Translator to avoid NullPointerException for multi-language errors
        ResourceBundleMessageSource messageSource = mock(ResourceBundleMessageSource.class);
        lenient().when(messageSource.getMessage(anyString(), any(), any())).thenReturn("Mocked Error Message");
        new Translator(messageSource);

        // Inject Kafka topic names using Reflection since @Value is not loaded in Mockito tests
        ReflectionTestUtils.setField(messageService, "chatTopic", "test-chat-topic");
        ReflectionTestUtils.setField(messageService, "systemTopic", "test-system-topic");

        recipientUser = new UserEntity();
        recipientUser.setUsername("recipient");
        recipientUser.setUserStatus(UserStatus.ACTIVE);
    }

    @Test
    void testSendPrivateMessage_RecipientNotFound() {
        when(userRepository.findByUsername("recipient")).thenReturn(Optional.empty());
        
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipient("recipient");

        assertThrows(ResourceNotFoundException.class, () -> messageService.sendPrivateMessage("sender", request));
    }

    @Test
    void testSendPrivateMessage_RecipientInactive() {
        recipientUser.setUserStatus(UserStatus.INACTIVE);
        when(userRepository.findByUsername("recipient")).thenReturn(Optional.of(recipientUser));
        
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipient("recipient");

        assertThrows(AccessForbiddenException.class, () -> messageService.sendPrivateMessage("sender", request));
    }

    @Test
    void testSendPrivateMessage_NotFriends() {
        when(userRepository.findByUsername("recipient")).thenReturn(Optional.of(recipientUser));
        when(friendService.isFriend("sender", "recipient")).thenReturn(false);
        
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipient("recipient");

        assertThrows(AccessForbiddenException.class, () -> messageService.sendPrivateMessage("sender", request));
    }

    @Test
    void testSendPrivateMessage_Success() {
        when(userRepository.findByUsername("recipient")).thenReturn(Optional.of(recipientUser));
        when(friendService.isFriend("sender", "recipient")).thenReturn(true);
        
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipient("recipient");
        request.setContent("Hello!");

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessageType(com.web.backend.common.MessageType.CHAT);
        when(messageMapper.toEntity(request)).thenReturn(chatMessage);
        
        // Mock Kafka future (asynchronous callback)
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class, RETURNS_DEEP_STUBS));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(future);

        // Mock Redis operations
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        // Act
        messageService.sendPrivateMessage("sender", request);

        // Assert
        verify(kafkaTemplate).send(eq("test-chat-topic"), eq(chatMessage));
        
        // Verify it was pushed to Redis list for caching
        verify(listOperations).rightPush(anyString(), eq(chatMessage));
        
        // Verify unread count was incremented
        verify(hashOperations).increment(anyString(), eq("sender"), eq(1L));
    }

    // ==========================================
    // TESTS FOR SYSTEM MESSAGE & REACTION
    // ==========================================

    @Test
    void testSendSystemMessage_Success() {
        com.web.backend.controller.request.MessageSystemRequest request = new com.web.backend.controller.request.MessageSystemRequest();
        request.setContent("System rebooting in 5 mins");
        request.setSurvivalTime(300L); // 5 minutes

        when(systemMessageRepository.save(any(com.web.backend.model.SystemMessage.class))).thenReturn(new com.web.backend.model.SystemMessage());
        
        com.web.backend.controller.response.MessageSystemResponse response = com.web.backend.controller.response.MessageSystemResponse.builder().build();
        when(messageMapper.systemMessageToResponse(any())).thenReturn(response);

        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class, RETURNS_DEEP_STUBS));
        when(kafkaTemplate.send(eq("test-system-topic"), any())).thenReturn(future);

        messageService.sendSystemMessage("adminUser", request);

        verify(systemMessageRepository).save(any(com.web.backend.model.SystemMessage.class));
        verify(kafkaTemplate).send(eq("test-system-topic"), eq(response));
    }

    @Test
    void testReactToMessage_NotFriends() {
        com.web.backend.controller.request.ReactionRequest request = new com.web.backend.controller.request.ReactionRequest();
        request.setRecipient("recipient");
        
        when(friendService.isFriend("sender", "recipient")).thenReturn(false);

        assertThrows(AccessForbiddenException.class, () -> messageService.reactToMessage("sender", request));
    }

    @Test
    void testReactToMessage_Success() {
        com.web.backend.controller.request.ReactionRequest request = new com.web.backend.controller.request.ReactionRequest();
        request.setRecipient("recipient");
        request.setMessageId("msg123");
        request.setReactionType(com.web.backend.common.ReactionType.HEART);

        when(friendService.isFriend("sender", "recipient")).thenReturn(true);
        
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class, RETURNS_DEEP_STUBS));
        when(kafkaTemplate.send(eq("test-chat-topic"), any())).thenReturn(future);

        messageService.reactToMessage("sender", request);

        // Verify MongoDB update
        verify(mongoTemplate).updateFirst(any(org.springframework.data.mongodb.core.query.Query.class), 
                                          any(org.springframework.data.mongodb.core.query.Update.class), 
                                          eq(ChatMessage.class));
                                          
        // Verify Redis deletion
        verify(redisTemplate).delete(eq("chat:recent:recipient_sender"));

        // Verify Kafka push
        verify(kafkaTemplate).send(eq("test-chat-topic"), any(ChatMessage.class));
    }

    // ==========================================
    // TESTS FOR EDIT, REVOKE & GET BY ID
    // ==========================================

    @Test
    void testEditMessage_Success() {
        EditMessageRequest request = new EditMessageRequest();
        request.setMessageId("msg1");
        request.setNewContent("Edited text");

        ChatMessage message = new ChatMessage();
        message.setId("msg1");
        message.setSender("sender");

        when(messageRepository.findById("msg1")).thenReturn(Optional.of(message));
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class, RETURNS_DEEP_STUBS));
        when(kafkaTemplate.send(eq("test-chat-topic"), any(ChatMessage.class))).thenReturn(future);

        messageService.editMessage("sender", request);

        assertTrue(message.isEdited());
        assertEquals("Edited text", message.getContent());
        verify(kafkaTemplate).send(eq("test-chat-topic"), eq(message));
    }

    @Test
    void testEditMessage_Forbidden() {
        EditMessageRequest request = new EditMessageRequest();
        request.setMessageId("msg1");

        ChatMessage message = new ChatMessage();
        message.setSender("otherUser");

        when(messageRepository.findById("msg1")).thenReturn(Optional.of(message));

        assertThrows(AccessForbiddenException.class, () -> messageService.editMessage("sender", request));
    }

    @Test
    void testRevokeMessage_Success() {
        RevokeMessageRequest request = new RevokeMessageRequest();
        request.setMessageId("msg1");

        ChatMessage message = new ChatMessage();
        message.setId("msg1");
        message.setSender("sender");
        message.setContent("Secret");
        message.setFileUrl("url");

        when(messageRepository.findById("msg1")).thenReturn(Optional.of(message));
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class, RETURNS_DEEP_STUBS));
        when(kafkaTemplate.send(eq("test-chat-topic"), any(ChatMessage.class))).thenReturn(future);

        messageService.revokeMessage("sender", request);

        assertTrue(message.isDeleted());
        assertEquals("", message.getContent());
        assertNull(message.getFileUrl());
        verify(kafkaTemplate).send(eq("test-chat-topic"), eq(message));
    }

    @Test
    void testGetMessageById_Success() {
        ChatMessage message = new ChatMessage();
        message.setId("msg1");
        message.setConversationId("sender_userB");

        when(messageRepository.findById("msg1")).thenReturn(Optional.of(message));
        ChatMessageResponse response = ChatMessageResponse.builder().build();
        when(messageMapper.toResponse(message)).thenReturn(response);

        ChatMessageResponse result = messageService.getMessageById("msg1", "sender");
        assertNotNull(result);
    }

    @Test
    void testGetMessageById_Forbidden() {
        ChatMessage message = new ChatMessage();
        message.setConversationId("userB_userC");

        when(messageRepository.findById("msg1")).thenReturn(Optional.of(message));

        assertThrows(AccessForbiddenException.class, () -> messageService.getMessageById("msg1", "sender"));
    }

    // ==========================================
    // TESTS FOR UNREAD COUNTS & READ STATUS
    // ==========================================

    @Test
    void testMarkMessagesAsRead_Success() {
        ChatMessage msg1 = new ChatMessage();
        msg1.setStatus(MessageStatus.SENT);

        when(messageRepository.findUnreadMessagesFromSender("recipient", "sender"))
                .thenReturn(List.of(msg1));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        messageService.markMessagesAsRead("recipient", "sender");

        assertEquals(MessageStatus.READ, msg1.getStatus());
        verify(messageRepository).saveAll(anyList());
        verify(hashOperations).delete("unread_counts:recipient", "sender");
        verify(kafkaTemplate).send(eq("test-chat-topic"), any(ChatMessage.class));
    }

    @Test
    void testGetUnreadMessageCounts_RedisHit() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        Map<Object, Object> cachedCounts = new HashMap<>();
        cachedCounts.put("senderA", "5");
        when(hashOperations.entries("unread_counts:recipient")).thenReturn(cachedCounts);

        UnreadCountsResponse response = messageService.getUnreadMessageCounts("recipient");
        assertEquals(5L, response.getUnreadCounts().get("senderA"));
        verify(messageRepository, never()).countUnreadMessagesBySender(anyString());
    }

    @Test
    void testGetUnreadMessageCounts_DbFallback() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries("unread_counts:recipient")).thenReturn(Collections.emptyMap());

        UnreadCountProjection projection = mock(UnreadCountProjection.class);
        when(projection.getSender()).thenReturn("senderA");
        when(projection.getCount()).thenReturn(3L);

        when(messageRepository.countUnreadMessagesBySender("recipient")).thenReturn(List.of(projection));

        UnreadCountsResponse response = messageService.getUnreadMessageCounts("recipient");
        assertEquals(3L, response.getUnreadCounts().get("senderA"));
        verify(hashOperations).putAll(eq("unread_counts:recipient"), anyMap());
    }

    // ==========================================
    // TESTS FOR CURSOR PAGINATION
    // ==========================================

    @Test
    void testFindSystemMessageWithCursor_FirstPage() {
        SystemMessage sysMsg1 = new SystemMessage();
        sysMsg1.setTimestamp(Instant.now());
        sysMsg1.setContent("Msg 1");
        
        when(systemMessageRepository.findInitialMessage(any(Pageable.class))).thenReturn(List.of(sysMsg1));
        MessageSystemResponse response = MessageSystemResponse.builder().content("Msg 1").build();
        when(messageMapper.systemMessageToResponse(sysMsg1)).thenReturn(response);

        CursorResponse<MessageSystemResponse> result = messageService.findSystemMessageWithCursor(null, 10);
        
        assertFalse(result.isHasMore());
        assertEquals(1, result.getContent().size());
    }

    @Test
    void testFindPrivateMessageWithCursor_FirstPage_MergeRedis() {
        ChatMessage dbMsg = new ChatMessage();
        dbMsg.setId("msg1");
        dbMsg.setTimestamp(LocalDateTime.now().minusDays(1));

        ChatMessage redisMsg = new ChatMessage();
        redisMsg.setId("msg2");
        redisMsg.setTimestamp(LocalDateTime.now());

        when(messageRepository.findByConversationId(eq("user1_user2"), any(Pageable.class)))
                .thenReturn(List.of(dbMsg));

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.range("chat:recent:user1_user2", 0, -1))
                .thenReturn(List.of(redisMsg));

        when(messageMapper.toResponse(any())).thenReturn(ChatMessageResponse.builder().build());

        CursorResponse<ChatMessageResponse> result = messageService.findPrivateMessageWithCursor("user2", "user1", null, 10);
        
        assertEquals(2, result.getContent().size());
    }
    @Test
    void testSendPrivateMessage_KafkaException() {
        when(userRepository.findByUsername("recipient")).thenReturn(Optional.of(recipientUser));
        when(friendService.isFriend("sender", "recipient")).thenReturn(true);
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipient("recipient");
        request.setContent("Hello!");

        ChatMessage chatMessage = new ChatMessage();
        when(messageMapper.toEntity(request)).thenReturn(chatMessage);
        
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(future);

        messageService.sendPrivateMessage("sender", request);
        verify(webSocketErrorHandler).handleChatError(eq("sender"), any(), anyString());
    }

    @Test
    void testSendPrivateMessage_RedisException() {
        when(userRepository.findByUsername("recipient")).thenReturn(Optional.of(recipientUser));
        when(friendService.isFriend("sender", "recipient")).thenReturn(true);
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipient("recipient");
        
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setMessageType(com.web.backend.common.MessageType.CHAT);
        when(messageMapper.toEntity(request)).thenReturn(chatMessage);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class, RETURNS_DEEP_STUBS));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(future);
        
        when(redisTemplate.opsForList()).thenThrow(new RuntimeException("Redis down"));

        // Should not throw exception to caller, just logs
        assertDoesNotThrow(() -> messageService.sendPrivateMessage("sender", request));
    }

    @Test
    void testSendSystemMessage_KafkaException() {
        com.web.backend.controller.request.MessageSystemRequest request = new com.web.backend.controller.request.MessageSystemRequest();
        request.setContent("Sys");

        when(systemMessageRepository.save(any())).thenReturn(new com.web.backend.model.SystemMessage());
        when(messageMapper.systemMessageToResponse(any())).thenReturn(com.web.backend.controller.response.MessageSystemResponse.builder().build());

        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(future);

        messageService.sendSystemMessage("admin", request);
        verify(webSocketErrorHandler).handleChatError(eq("admin"), any(), anyString());
    }

    @Test
    void testReactToMessage_RemoveReaction() {
        com.web.backend.controller.request.ReactionRequest request = new com.web.backend.controller.request.ReactionRequest();
        request.setRecipient("recipient");
        request.setMessageId("msg1");
        request.setReactionType(null); // Removes reaction

        when(friendService.isFriend("sender", "recipient")).thenReturn(true);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mock(SendResult.class, RETURNS_DEEP_STUBS));
        when(kafkaTemplate.send(anyString(), any())).thenReturn(future);

        messageService.reactToMessage("sender", request);
        verify(mongoTemplate).updateFirst(any(), any(), eq(ChatMessage.class));
    }

    @Test
    void testFindPrivateMessageWithCursor_WithCursorAndHasMore() {
        ChatMessage dbMsg = new ChatMessage();
        dbMsg.setId("msg1");
        dbMsg.setTimestamp(LocalDateTime.now().minusDays(1));

        List<ChatMessage> mockResult = new java.util.ArrayList<>();
        for (int i=0; i<11; i++) mockResult.add(dbMsg); // 11 elements means hasMore = true
        
        when(messageRepository.findByConversationIdAndTimestampBefore(anyString(), any(), any()))
                .thenReturn(mockResult);
        when(messageMapper.toResponse(any())).thenReturn(ChatMessageResponse.builder().build());

        CursorResponse<ChatMessageResponse> result = messageService.findPrivateMessageWithCursor("user2", "user1", LocalDateTime.now().toString(), 10);
        
        assertTrue(result.isHasMore());
        assertEquals(10, result.getContent().size()); // should have removed the 11th
    }
}

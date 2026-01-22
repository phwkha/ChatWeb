package com.web.backend.repository;

import com.web.backend.controller.response.UnreadCountResultResponse;
import com.web.backend.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<ChatMessage, String> {

    @Query("{ 'conversationId': ?0, 'messageType': 'PRIVATE_CHAT' }")
    List<ChatMessage> findByConversationId(String conversationId, Pageable pageable);

    @Query("{ 'conversationId': ?0, 'messageType': 'PRIVATE_CHAT', 'timestamp': { '$lt': ?1 } }")
    List<ChatMessage> findByConversationIdAndTimestampBefore(String conversationId, LocalDateTime cursor, Pageable pageable);

    @Query("{ 'messageType': 'CHAT' }")
    List<ChatMessage> findInitialMessageByMessageTypeIsChat(Pageable pageable);

    @Query("{ 'messageType': 'CHAT', 'timestamp': { '$lt': ?0 } }")
    List<ChatMessage> findMessageByMessageTypeIsChat(LocalDateTime cursor, Pageable pageable);

    @Query("{ 'recipient': ?0, 'sender': ?1, 'isRead': false, 'messageType': 'PRIVATE_CHAT' }")
    List<ChatMessage> findUnreadMessagesFromSender(String recipient, String sender);

    @Query(value = "{ $or: [ { 'sender': ?0 }, { 'recipient': ?0 } ] }", exists = true)
    boolean existsBySenderOrRecipient(String username);

    @Aggregation(pipeline = {
            "{ '$match': { 'recipient': ?0, 'status': 'SENT', 'messageType': 'PRIVATE_CHAT' } }",
            "{ '$group': { '_id': '$sender', 'count': { '$sum': 1 } } }",
            "{ '$project': { 'senderId': '$_id', 'count': 1, '_id': 0 } }"
    })
    List<UnreadCountResultResponse> countUnreadMessagesBySender(String recipientUsername);
}

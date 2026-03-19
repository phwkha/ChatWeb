package com.web.backend.repository;

import com.web.backend.repository.projection.UnreadCountProjection;
import com.web.backend.model.ChatMessage;
import org.springframework.data.domain.Pageable;
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

    @Query("{ 'recipient': ?0, 'sender': ?1, 'status': 'SENT', 'messageType': 'PRIVATE_CHAT' }")
    List<ChatMessage> findUnreadMessagesFromSender(String recipient, String sender);

    @Query(value = "{ $or: [ { 'sender': ?0 }, { 'recipient': ?0 } ] }", exists = true)
    boolean existsBySenderOrRecipient(String username);

    @Aggregation(pipeline = {
            "{ '$match': { 'recipient': ?0, 'status': 'SENT', 'messageType': 'PRIVATE_CHAT' } }",
            "{ '$group': { '_id': '$sender', 'count': { '$sum': 1 } } }",
            "{ '$project': { 'sender': '$_id', 'count': 1, '_id': 0 } }"
    })
    List<UnreadCountProjection> countUnreadMessagesBySender(String recipientUsername);
}

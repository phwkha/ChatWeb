package com.web.backend.repository;

import com.web.backend.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<ChatMessage, String> {

    @Query("{ 'messageType': 'PRIVATE_CHAT', '$or': [ { 'sender': ?0, 'recipient': ?1 }, { 'sender': ?1, 'recipient': ?0 } ] }")
    List<ChatMessage> findInitialMessages(String user1, String user2, Pageable pageable);

    @Query("{ 'messageType': 'PRIVATE_CHAT', 'timestamp': { '$lt': ?2 }, '$or': [ { 'sender': ?0, 'recipient': ?1 }, { 'sender': ?1, 'recipient': ?0 } ] }")
    List<ChatMessage> findMessagesBeforeCursor(String user1, String user2, LocalDateTime cursor, Pageable pageable);

    @Query("{ 'messageType': 'CHAT' }")
    List<ChatMessage> findInitialMessageByMessageTypeIsChat(Pageable pageable);

    @Query("{ 'messageType': 'CHAT', 'timestamp': { '$lt': ?0 } }")
    List<ChatMessage> findMessageByMessageTypeIsChat(LocalDateTime cursor, Pageable pageable);

    @Query("{ 'recipient': ?0, 'isRead': false, 'messageType': 'PRIVATE_CHAT' }")
    List<ChatMessage> findUnreadPrivateMessages(String recipientUsername);

    @Query("{ 'recipient': ?0, 'sender': ?1, 'isRead': false, 'messageType': 'PRIVATE_CHAT' }")
    List<ChatMessage> findUnreadMessagesFromSender(String recipient, String sender);

    @Query(value = "{ $or: [ { 'sender': ?0 }, { 'recipient': ?0 } ] }", exists = true)
    boolean existsBySenderOrRecipient(String username);
}

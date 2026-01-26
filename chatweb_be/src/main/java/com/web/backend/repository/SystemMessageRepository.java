package com.web.backend.repository;

import com.web.backend.model.ChatMessage;
import com.web.backend.model.SystemMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemMessageRepository extends MongoRepository<SystemMessage, String> {

    List<ChatMessage> findInitialMessage(Pageable pageable);

    @Query("'timestamp': { '$lt': ?0 } }")
    List<ChatMessage> findMessage(LocalDateTime cursor, Pageable pageable);
}

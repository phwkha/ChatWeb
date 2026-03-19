package com.web.backend.repository;

import com.web.backend.model.SystemMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SystemMessageRepository extends MongoRepository<SystemMessage, String> {

    @Query("{}")
    List<SystemMessage> findInitialMessage(Pageable pageable);

    @Query("'timestamp': { '$lt': ?0 } }")
    List<SystemMessage> findMessage(Instant cursor, Pageable pageable);
}

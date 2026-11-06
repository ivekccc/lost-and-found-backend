package com.example.demo.messaging.repository;

import com.example.demo.messaging.model.Message;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.id DESC")
    List<Message> findLatest(@Param("conversationId") Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId AND m.id < :beforeId "
            + "ORDER BY m.id DESC")
    List<Message> findBefore(@Param("conversationId") Long conversationId,
                             @Param("beforeId") Long beforeId,
                             Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.id ASC")
    List<Message> findAllOfConversation(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.id IN ("
            + "SELECT MAX(m2.id) FROM Message m2 WHERE m2.conversation.id IN :conversationIds "
            + "GROUP BY m2.conversation.id)")
    List<Message> findLastOfConversations(@Param("conversationIds") List<Long> conversationIds);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId "
            + "AND m.sender.id <> :viewerId")
    long countFromOthers(@Param("conversationId") Long conversationId,
                         @Param("viewerId") Long viewerId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId "
            + "AND m.sender.id <> :viewerId AND m.createdAt > :since")
    long countFromOthersSince(@Param("conversationId") Long conversationId,
                              @Param("viewerId") Long viewerId,
                              @Param("since") LocalDateTime since);

    long countBySenderIdAndCreatedAtAfter(Long senderId, LocalDateTime after);

    @Query("SELECT m FROM Message m JOIN FETCH m.conversation c JOIN FETCH c.holder JOIN FETCH c.claimant "
            + "WHERE m.id = :id")
    Optional<Message> findWithConversation(@Param("id") Long id);
}

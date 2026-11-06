package com.example.demo.messaging.repository;

import com.example.demo.messaging.model.Conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    boolean existsByClaimId(Long claimId);

    Optional<Conversation> findByClaimId(Long claimId);

    @Query("SELECT c FROM Conversation c "
            + "JOIN FETCH c.holder "
            + "JOIN FETCH c.claimant "
            + "WHERE c.holder.id = :userId OR c.claimant.id = :userId "
            + "ORDER BY COALESCE(c.lastMessageAt, c.createdAt) DESC")
    List<Conversation> findAllOfUser(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c "
            + "JOIN FETCH c.holder "
            + "JOIN FETCH c.claimant "
            + "WHERE c.id = :id AND (c.holder.id = :userId OR c.claimant.id = :userId)")
    Optional<Conversation> findOfParticipant(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c "
            + "JOIN FETCH c.holder "
            + "JOIN FETCH c.claimant "
            + "WHERE c.id = :id")
    Optional<Conversation> findWithParticipants(@Param("id") Long id);

    @Modifying
    @Query("DELETE FROM Conversation c WHERE c.holder.id = :userId OR c.claimant.id = :userId")
    int deleteAllOfUser(@Param("userId") Long userId);

    @Query(value = "SELECT r.title AS title, r.type AS type, r.user_id AS \"ownerId\", "
            + "r.contact_email AS \"contactEmail\", r.contact_phone AS \"contactPhone\", "
            + "z.city_id AS \"cityId\" "
            + "FROM reports r "
            + "LEFT JOIN locations l ON l.id = r.location_id "
            + "LEFT JOIN zones z ON z.id = l.zone_id "
            + "WHERE r.id = :reportId",
            nativeQuery = true)
    Optional<ConversationReportView> findReportView(@Param("reportId") Long reportId);
}

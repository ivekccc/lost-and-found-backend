package com.example.demo.user.repository;

import com.example.demo.user.model.RefreshToken;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RefreshToken t JOIN FETCH t.user WHERE t.tokenHash = :tokenHash")
    Optional<RefreshToken> findForRotation(@Param("tokenHash") String tokenHash);

    @Query("""
            SELECT COUNT(DISTINCT t.familyId) FROM RefreshToken t
            WHERE t.user.id = :userId AND t.consumedAt IS NULL AND t.expiresAt > :now AND t.familyExpiresAt > :now
            """)
    long countActiveSessions(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.familyId = :familyId")
    int deleteFamily(@Param("familyId") UUID familyId);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiresAt < :now OR t.familyExpiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}

package com.example.demo.user.repository;

import com.example.demo.user.model.RefreshToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    long countByUserIdAndExpiresAtAfter(Long userId, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.tokenHash = :tokenHash AND t.expiresAt > :now")
    int deleteLiveByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Modifying
    int deleteByExpiresAtBefore(LocalDateTime dateTime);
}

package com.example.demo.repository;

import com.example.demo.model.ContactReveal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ContactRevealRepository extends JpaRepository<ContactReveal, Long> {

    @Query("SELECT COUNT(cr) FROM ContactReveal cr WHERE cr.user.id = :userId AND cr.revealedAt >= :since")
    long countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}

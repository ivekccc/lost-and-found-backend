package com.example.demo.repository;

import com.example.demo.model.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    Optional<PasswordReset> findByEmail(String email);

    @Modifying
    @Query("DELETE FROM PasswordReset p WHERE p.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Modifying
    int deleteByExpiresAtBefore(LocalDateTime dateTime);
}

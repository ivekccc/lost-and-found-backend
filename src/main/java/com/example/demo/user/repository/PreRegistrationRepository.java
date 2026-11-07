package com.example.demo.user.repository;

import com.example.demo.user.model.PreRegistration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
public interface PreRegistrationRepository extends  JpaRepository<PreRegistration,Long> {
    Optional<PreRegistration> findByEmail(String email);

    @Modifying
    @Query("DELETE FROM PreRegistration p WHERE p.email = :email")
    void deleteByEmail(String email);

    @Modifying
    int deleteByExpiresAtBefore(LocalDateTime dateTime);
}

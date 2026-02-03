package com.example.demo.repository;


import com.example.demo.model.PreRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public interface PreRegistrationRepository extends  JpaRepository<PreRegistration,Long> {
    Optional<PreRegistration> findByEmail(String email);
    Optional<PreRegistration> findByEmailAndVerificationCode(String email, String verificationCode);

    void deleteByExpiresAtBefore(LocalDateTime dateTime);
}

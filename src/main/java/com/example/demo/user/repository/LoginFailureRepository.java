package com.example.demo.user.repository;

import com.example.demo.user.model.LoginFailure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface LoginFailureRepository extends JpaRepository<LoginFailure, Long> {

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);

    @Query("SELECT MIN(f.createdAt) FROM LoginFailure f WHERE f.email = :email AND f.createdAt > :after")
    LocalDateTime findOldestSince(@Param("email") String email, @Param("after") LocalDateTime after);

    @Modifying
    @Query("DELETE FROM LoginFailure f WHERE f.email = :email")
    void deleteByEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM LoginFailure f WHERE f.createdAt < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}

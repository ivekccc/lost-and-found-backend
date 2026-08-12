package com.example.demo.user.repository;

import com.example.demo.user.model.User;
import com.example.demo.user.model.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByUsername(String username);

    List<User> findByFcmTokenIsNotNull();

    List<User> findByRole(UserRole role);
}

package com.example.demo.user.repository;

import com.example.demo.user.model.User;
import com.example.demo.user.model.UserRole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByUsername(String username);

    List<User> findByFcmTokenIsNotNull();

    @Modifying
    @Query("UPDATE User u SET u.fcmToken = null WHERE u.fcmToken = :fcmToken AND u.id <> :userId")
    int detachPushTokenFromOtherUsers(@Param("fcmToken") String fcmToken, @Param("userId") Long userId);

    List<User> findByRole(UserRole role);
}

package com.example.demo.user.repository;

import com.example.demo.shared.stats.DailyCountView;
import com.example.demo.user.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    @Query(value = "SELECT CAST(u.created_at AS date) AS day, COUNT(*) AS count FROM users u "
            + "WHERE u.created_at >= :from GROUP BY CAST(u.created_at AS date)", nativeQuery = true)
    List<DailyCountView> countRegisteredPerDay(@Param("from") LocalDateTime from);

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByUsername(String username);

    List<User> findByFcmTokenIsNotNull();

    @Modifying
    @Query("UPDATE User u SET u.fcmToken = null WHERE u.fcmToken = :fcmToken AND u.id <> :userId")
    int detachPushTokenFromOtherUsers(@Param("fcmToken") String fcmToken, @Param("userId") Long userId);

}

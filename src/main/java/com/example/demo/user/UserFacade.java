package com.example.demo.user;

import com.example.demo.user.model.User;

/**
 * The only entry point other modules may use to reach users. Everything below this package —
 * repositories, services, entities — is internal to the module.
 *
 * Returns the {@code User} entity rather than a DTO because callers attach it to JPA relations
 * ({@code Report.user}, {@code Notification.user}). Turning those into plain ids would mean
 * rewriting every query, fetch join and lock in the application.
 */
public interface UserFacade {

    User requireByEmail(String email);

    User requireById(Long userId);

    void savePushToken(String email, String fcmToken);

    /** Called after a push is rejected, so the dead token is not retried forever. */
    void clearPushToken(Long userId);
}

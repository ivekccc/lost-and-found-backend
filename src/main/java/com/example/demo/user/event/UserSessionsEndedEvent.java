package com.example.demo.user.event;

public record UserSessionsEndedEvent(Long userId, String email) {
}

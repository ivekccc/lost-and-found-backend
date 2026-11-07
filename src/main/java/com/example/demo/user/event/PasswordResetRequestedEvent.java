package com.example.demo.user.event;

import java.util.Locale;

public record PasswordResetRequestedEvent(String email, String code, Locale locale) {
}

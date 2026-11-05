package com.example.demo.shared.exception;

import lombok.Getter;
@Getter
public class RateLimitExceededException extends LocalizedException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String messageKey, long retryAfterSeconds) {
        super(messageKey);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}

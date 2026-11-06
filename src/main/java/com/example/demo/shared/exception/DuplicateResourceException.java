package com.example.demo.shared.exception;

public class DuplicateResourceException extends LocalizedException {
    public DuplicateResourceException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

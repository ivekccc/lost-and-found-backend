package com.example.demo.shared.exception;

public class InvalidSavedSearchException extends LocalizedException {
    public InvalidSavedSearchException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

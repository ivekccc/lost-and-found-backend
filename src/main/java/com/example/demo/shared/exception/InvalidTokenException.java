package com.example.demo.shared.exception;
public class InvalidTokenException extends LocalizedException {
    public InvalidTokenException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

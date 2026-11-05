package com.example.demo.shared.exception;
public class UserAlreadyExistsException extends LocalizedException {
    public UserAlreadyExistsException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

package com.example.demo.shared.exception;
public class ResourceNotFoundException extends LocalizedException {
    public ResourceNotFoundException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

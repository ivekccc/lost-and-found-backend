package com.example.demo.shared.exception;

public class InvalidChatMessageException extends LocalizedException {
    public InvalidChatMessageException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

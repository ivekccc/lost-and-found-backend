package com.example.demo.shared.exception;
public class EmailSendException extends LocalizedException {
    public EmailSendException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

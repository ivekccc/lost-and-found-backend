package com.example.demo.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidVerificationException extends LocalizedException {
    public InvalidVerificationException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

package com.example.demo.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidClaimException extends LocalizedException {
    public InvalidClaimException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

package com.example.demo.shared.exception;

import lombok.Getter;

@Getter
public abstract class LocalizedException extends RuntimeException {

    private final transient Object[] arguments;

    protected LocalizedException(String messageKey, Object... arguments) {
        super(messageKey);
        this.arguments = arguments;
    }
}

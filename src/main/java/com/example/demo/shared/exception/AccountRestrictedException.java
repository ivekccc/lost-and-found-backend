package com.example.demo.shared.exception;
public class AccountRestrictedException extends LocalizedException {
    public AccountRestrictedException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

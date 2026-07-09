package com.example.demo.exception;

public class AccountRestrictedException extends RuntimeException {
    public AccountRestrictedException(String message) {
        super(message);
    }
}

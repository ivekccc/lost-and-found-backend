package com.example.demo.exception;

public class InvalidAbuseReportException extends RuntimeException {
    public InvalidAbuseReportException(String message) {
        super(message);
    }
}

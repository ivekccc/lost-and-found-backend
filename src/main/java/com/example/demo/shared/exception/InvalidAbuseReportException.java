package com.example.demo.shared.exception;
public class InvalidAbuseReportException extends LocalizedException {
    public InvalidAbuseReportException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

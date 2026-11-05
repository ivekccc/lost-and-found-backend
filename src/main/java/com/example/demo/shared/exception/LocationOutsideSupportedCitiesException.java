package com.example.demo.shared.exception;

public class LocationOutsideSupportedCitiesException extends LocalizedException {

    public LocationOutsideSupportedCitiesException(String messageKey, Object... arguments) {
        super(messageKey, arguments);
    }
}

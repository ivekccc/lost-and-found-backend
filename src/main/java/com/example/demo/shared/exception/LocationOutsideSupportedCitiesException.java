package com.example.demo.shared.exception;

public class LocationOutsideSupportedCitiesException extends RuntimeException {

    public LocationOutsideSupportedCitiesException(String message) {
        super(message);
    }
}

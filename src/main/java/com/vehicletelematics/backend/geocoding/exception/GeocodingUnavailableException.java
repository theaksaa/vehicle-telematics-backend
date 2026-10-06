package com.vehicletelematics.backend.geocoding.exception;

public class GeocodingUnavailableException extends RuntimeException {

    public GeocodingUnavailableException(Throwable cause) {
        super("Reverse geocoding service is temporarily unavailable", cause);
    }
}

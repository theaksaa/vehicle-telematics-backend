package com.vehicletelematics.backend.geocoding.exception;

public class LocationNotFoundException extends RuntimeException {

    public LocationNotFoundException() {
        super("No address was found for the supplied coordinates");
    }
}

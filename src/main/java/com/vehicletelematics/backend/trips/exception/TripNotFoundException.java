package com.vehicletelematics.backend.trips.exception;

public class TripNotFoundException extends RuntimeException {
    public TripNotFoundException(long id) {
        super("Trip " + id + " was not found");
    }
}

package com.vehicletelematics.backend.vehicles.exception;

public class VinAlreadyExistsException extends RuntimeException {
    public VinAlreadyExistsException(String vin) {
        super("Vehicle VIN already exists: " + vin);
    }
}

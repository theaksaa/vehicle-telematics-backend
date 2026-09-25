package com.vehicletelematics.backend.vehicles.exception;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(long id) {
        super("Vehicle not found: " + id);
    }
}

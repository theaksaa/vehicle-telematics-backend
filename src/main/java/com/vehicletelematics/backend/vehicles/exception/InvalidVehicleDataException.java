package com.vehicletelematics.backend.vehicles.exception;

public class InvalidVehicleDataException extends RuntimeException {
    public InvalidVehicleDataException(String message) {
        super(message);
    }
}

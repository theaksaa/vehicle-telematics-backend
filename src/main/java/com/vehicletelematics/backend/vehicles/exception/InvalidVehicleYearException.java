package com.vehicletelematics.backend.vehicles.exception;

public class InvalidVehicleYearException extends RuntimeException {
    public InvalidVehicleYearException(int maximumYear) {
        super("Vehicle year must be between 1900 and " + maximumYear);
    }
}

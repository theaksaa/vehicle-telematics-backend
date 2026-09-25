package com.vehicletelematics.backend.vehicles.exception;

public class RegistrationAlreadyExistsException extends RuntimeException {
    public RegistrationAlreadyExistsException(String registration) {
        super("Vehicle registration already exists: " + registration);
    }
}

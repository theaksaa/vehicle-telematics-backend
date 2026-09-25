package com.vehicletelematics.backend.devices.exception;

public class InvalidDeviceDataException extends RuntimeException {
    public InvalidDeviceDataException(String message) {
        super(message);
    }
}

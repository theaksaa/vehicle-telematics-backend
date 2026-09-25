package com.vehicletelematics.backend.devices.exception;

public class DeviceAssignmentConflictException extends RuntimeException {
    public DeviceAssignmentConflictException(String message) {
        super(message);
    }
}

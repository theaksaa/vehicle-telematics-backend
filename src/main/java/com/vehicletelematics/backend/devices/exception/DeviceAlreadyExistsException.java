package com.vehicletelematics.backend.devices.exception;

public class DeviceAlreadyExistsException extends RuntimeException {
    public DeviceAlreadyExistsException(String deviceId) {
        super("Device ID already exists: " + deviceId);
    }
}

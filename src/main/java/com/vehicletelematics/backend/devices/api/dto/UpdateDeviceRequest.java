package com.vehicletelematics.backend.devices.api.dto;

import com.vehicletelematics.backend.devices.domain.DeviceStatus;

import java.time.Instant;

public record UpdateDeviceRequest(
        String deviceId,
        DeviceStatus status,
        Instant lastSeenAt,
        Long lastBootId,
        String firmwareVersion) {
}

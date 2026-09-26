package com.vehicletelematics.backend.websocket.dto;

import com.vehicletelematics.backend.devices.domain.DeviceStatus;

import java.time.Instant;

public record DeviceStateWebSocketMessage(
        String deviceId,
        DeviceStatus status,
        Instant lastSeenAt,
        Long lastBootId) {
}

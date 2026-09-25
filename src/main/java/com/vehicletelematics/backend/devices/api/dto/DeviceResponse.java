package com.vehicletelematics.backend.devices.api.dto;

import tools.jackson.databind.JsonNode;
import com.vehicletelematics.backend.devices.domain.Device;
import com.vehicletelematics.backend.devices.domain.DeviceConfigState;
import com.vehicletelematics.backend.devices.domain.DeviceStatus;

import java.time.Instant;

public record DeviceResponse(
        Long id,
        String deviceId,
        Long vehicleId,
        DeviceStatus status,
        Instant lastSeenAt,
        Long lastBootId,
        String firmwareVersion,
        DeviceConfigState configState,
        Long desiredConfigVersion,
        JsonNode desiredConfig,
        Instant desiredConfigUpdatedAt,
        Long reportedConfigVersion,
        JsonNode reportedConfig,
        Instant reportedConfigAt,
        Instant createdAt,
        Instant updatedAt) {

    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getDeviceId(),
                device.getVehicle() == null ? null : device.getVehicle().getId(),
                device.getStatus(),
                device.getLastSeenAt(),
                device.getLastBootId(),
                device.getFirmwareVersion(),
                device.getConfigState(),
                device.getDesiredConfigVersion(),
                device.getDesiredConfig(),
                device.getDesiredConfigUpdatedAt(),
                device.getReportedConfigVersion(),
                device.getReportedConfig(),
                device.getReportedConfigAt(),
                device.getCreatedAt(),
                device.getUpdatedAt());
    }
}

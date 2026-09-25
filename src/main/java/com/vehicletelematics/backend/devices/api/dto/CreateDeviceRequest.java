package com.vehicletelematics.backend.devices.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDeviceRequest(
        @NotBlank String deviceId,
        String firmwareVersion) {
}

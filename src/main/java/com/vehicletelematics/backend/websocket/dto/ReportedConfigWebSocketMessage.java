package com.vehicletelematics.backend.websocket.dto;

import com.vehicletelematics.backend.devices.domain.DeviceConfigState;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

public record ReportedConfigWebSocketMessage(
        String deviceId,
        Long version,
        JsonNode config,
        Instant reportedAt,
        DeviceConfigState state) {
}

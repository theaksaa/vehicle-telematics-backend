package com.vehicletelematics.backend.telemetry.service;

import java.time.Instant;

public record TelemetryMessage(
        short protocolVersion,
        long bootId,
        long sequenceNumber,
        Instant recordedAt,
        Instant receivedAt,
        Double latitude,
        Double longitude,
        Float gnssSpeedKph,
        Float vehicleSpeedKph,
        Integer rpm,
        Float acceleratorPct,
        Short rssiDbm) {
}

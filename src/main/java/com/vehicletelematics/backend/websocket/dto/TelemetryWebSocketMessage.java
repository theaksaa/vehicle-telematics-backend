package com.vehicletelematics.backend.websocket.dto;

import com.vehicletelematics.backend.telemetry.domain.Telemetry;

import java.time.Instant;

public record TelemetryWebSocketMessage(
        Long id,
        String deviceId,
        Long vehicleId,
        Long tripId,
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

    public static TelemetryWebSocketMessage from(Telemetry telemetry) {
        return new TelemetryWebSocketMessage(
                telemetry.getId(),
                telemetry.getDevice().getDeviceId(),
                telemetry.getVehicle() == null ? null : telemetry.getVehicle().getId(),
                telemetry.getTripId(),
                telemetry.getProtocolVersion(),
                telemetry.getBootId(),
                telemetry.getSequenceNumber(),
                telemetry.getRecordedAt(),
                telemetry.getReceivedAt(),
                telemetry.getLatitude(),
                telemetry.getLongitude(),
                telemetry.getGnssSpeedKph(),
                telemetry.getVehicleSpeedKph(),
                telemetry.getRpm(),
                telemetry.getAcceleratorPct(),
                telemetry.getRssiDbm());
    }
}

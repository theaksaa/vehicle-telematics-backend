package com.vehicletelematics.backend.vehicles.api.dto;

import com.vehicletelematics.backend.vehicles.state.VehicleState;

import java.time.Instant;

public record VehicleStateResponse(
        Long vehicleId,
        boolean online,
        Float speedKph,
        Integer rpm,
        Double latitude,
        Double longitude,
        Instant lastTelemetryAt,
        Instant lastLocationAt,
        Instant updatedAt) {

    public static VehicleStateResponse from(VehicleState state) {
        return new VehicleStateResponse(
                state.getVehicleId(), state.isOnline(), state.getSpeedKph(), state.getRpm(),
                state.getLatitude(), state.getLongitude(), state.getLastTelemetryAt(),
                state.getLastLocationAt(), state.getUpdatedAt());
    }
}

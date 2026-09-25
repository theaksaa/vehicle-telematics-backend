package com.vehicletelematics.backend.vehicles.api.dto;

import com.vehicletelematics.backend.vehicles.domain.Vehicle;

import java.time.Instant;

public record VehicleResponse(
        Long id,
        String registration,
        String manufacturer,
        String model,
        Short year,
        String vin,
        String description,
        boolean active,
        String deviceId,
        Instant createdAt,
        Instant updatedAt) {

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getRegistration(),
                vehicle.getManufacturer(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getVin(),
                vehicle.getDescription(),
                vehicle.isActive(),
                vehicle.getDevice() == null ? null : vehicle.getDevice().getDeviceId(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt());
    }
}

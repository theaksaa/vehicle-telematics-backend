package com.vehicletelematics.backend.vehicles.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateVehicleRequest(
        @NotBlank String registration,
        @NotBlank String manufacturer,
        @NotBlank String model,
        Short year,
        String vin,
        String description) {
}

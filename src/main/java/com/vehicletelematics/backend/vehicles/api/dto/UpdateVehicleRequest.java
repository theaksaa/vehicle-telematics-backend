package com.vehicletelematics.backend.vehicles.api.dto;

import jakarta.validation.constraints.Pattern;

public record UpdateVehicleRequest(
        @Pattern(regexp = ".*\\S.*") String registration,
        @Pattern(regexp = ".*\\S.*") String manufacturer,
        @Pattern(regexp = ".*\\S.*") String model,
        Short year,
        String vin,
        String description,
        Boolean active) {
}

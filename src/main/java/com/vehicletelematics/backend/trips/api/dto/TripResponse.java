package com.vehicletelematics.backend.trips.api.dto;

import com.vehicletelematics.backend.trips.domain.Trip;
import com.vehicletelematics.backend.trips.domain.TripStatus;

import java.time.Instant;

public record TripResponse(
        Long id,
        Long vehicleId,
        Instant startedAt,
        Instant endedAt,
        Instant lastTelemetryAt,
        TripStatus status,
        Double startLatitude,
        Double startLongitude,
        Double endLatitude,
        Double endLongitude,
        long telemetryCount) {

    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(), trip.getVehicle().getId(), trip.getStartedAt(), trip.getEndedAt(),
                trip.getLastTelemetryAt(), trip.getStatus(), trip.getStartLatitude(), trip.getStartLongitude(),
                trip.getEndLatitude(), trip.getEndLongitude(), trip.getTelemetryCount());
    }
}

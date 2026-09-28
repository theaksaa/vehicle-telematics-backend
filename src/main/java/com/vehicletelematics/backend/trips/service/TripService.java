package com.vehicletelematics.backend.trips.service;

import com.vehicletelematics.backend.common.api.PageResponse;
import com.vehicletelematics.backend.telemetry.domain.Telemetry;
import com.vehicletelematics.backend.trips.api.dto.TripResponse;
import com.vehicletelematics.backend.trips.domain.Trip;
import com.vehicletelematics.backend.trips.domain.TripStatus;
import com.vehicletelematics.backend.trips.exception.TripNotFoundException;
import com.vehicletelematics.backend.trips.repository.TripRepository;
import com.vehicletelematics.backend.vehicles.exception.VehicleNotFoundException;
import com.vehicletelematics.backend.vehicles.repository.VehicleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;

@Service
public class TripService {

    private static final Duration MAX_TELEMETRY_GAP = Duration.ofMinutes(5);
    private static final float MOVEMENT_THRESHOLD_KPH = 0.5f;

    private final TripRepository tripRepository;
    private final VehicleRepository vehicleRepository;

    public TripService(TripRepository tripRepository, VehicleRepository vehicleRepository) {
        this.tripRepository = tripRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public void assign(Telemetry telemetry) {
        if (telemetry.getVehicle() == null) {
            return;
        }
        long vehicleId = telemetry.getVehicle().getId();
        Optional<Trip> current = tripRepository.findFirstByVehicleIdAndStatusOrderByStartedAtDesc(
                vehicleId, TripStatus.OPEN);

        if (current.isPresent() && telemetry.getRecordedAt().isAfter(
                current.get().getLastTelemetryAt().plus(MAX_TELEMETRY_GAP))) {
            current.get().close(current.get().getLastTelemetryAt());
            current = Optional.empty();
        }

        boolean active = isActive(telemetry);
        if (active && current.isEmpty()) {
            Trip trip = tripRepository.save(new Trip(telemetry.getVehicle(), telemetry));
            telemetry.assignToTrip(trip.getId());
            return;
        }
        if (current.isEmpty()) {
            return;
        }

        Trip trip = current.get();
        trip.observe(telemetry);
        telemetry.assignToTrip(trip.getId());
        if (!active) {
            trip.close(telemetry.getRecordedAt());
        }
    }

    @Transactional
    public void closeOpenTrip(long vehicleId) {
        tripRepository.findFirstByVehicleIdAndStatusOrderByStartedAtDesc(vehicleId, TripStatus.OPEN)
                .ifPresent(trip -> trip.close(trip.getLastTelemetryAt()));
    }

    @Transactional(readOnly = true)
    public PageResponse<TripResponse> findByVehicle(long vehicleId, int page, int size) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new VehicleNotFoundException(vehicleId);
        }
        return PageResponse.from(tripRepository.findByVehicleIdOrderByStartedAtDesc(
                vehicleId, PageRequest.of(page, size)).map(TripResponse::from));
    }

    @Transactional(readOnly = true)
    public TripResponse find(long tripId) {
        return TripResponse.from(tripRepository.findById(tripId)
                .orElseThrow(() -> new TripNotFoundException(tripId)));
    }

    private boolean isActive(Telemetry telemetry) {
        return (telemetry.getRpm() != null && telemetry.getRpm() > 0)
                || (telemetry.getVehicleSpeedKph() != null
                && telemetry.getVehicleSpeedKph() > MOVEMENT_THRESHOLD_KPH)
                || (telemetry.getGnssSpeedKph() != null
                && telemetry.getGnssSpeedKph() > MOVEMENT_THRESHOLD_KPH);
    }
}

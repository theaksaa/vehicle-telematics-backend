package com.vehicletelematics.backend.telemetry.service;

import com.vehicletelematics.backend.devices.domain.Device;
import com.vehicletelematics.backend.devices.exception.DeviceNotFoundException;
import com.vehicletelematics.backend.devices.repository.DeviceRepository;
import com.vehicletelematics.backend.common.api.PageResponse;
import com.vehicletelematics.backend.telemetry.api.dto.TelemetryResponse;
import com.vehicletelematics.backend.telemetry.domain.Telemetry;
import com.vehicletelematics.backend.telemetry.repository.TelemetryRepository;
import com.vehicletelematics.backend.trips.exception.TripNotFoundException;
import com.vehicletelematics.backend.trips.repository.TripRepository;
import com.vehicletelematics.backend.trips.service.TripService;
import com.vehicletelematics.backend.vehicles.exception.VehicleNotFoundException;
import com.vehicletelematics.backend.vehicles.repository.VehicleRepository;
import com.vehicletelematics.backend.vehicles.state.VehicleStateService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
public class TelemetryService {

    private final DeviceRepository deviceRepository;
    private final TelemetryRepository telemetryRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final TripService tripService;
    private final VehicleStateService vehicleStateService;

    public TelemetryService(
            DeviceRepository deviceRepository,
            TelemetryRepository telemetryRepository,
            VehicleRepository vehicleRepository,
            TripRepository tripRepository,
            TripService tripService,
            VehicleStateService vehicleStateService) {
        this.deviceRepository = deviceRepository;
        this.telemetryRepository = telemetryRepository;
        this.vehicleRepository = vehicleRepository;
        this.tripRepository = tripRepository;
        this.tripService = tripService;
        this.vehicleStateService = vehicleStateService;
    }

    @Transactional
    public Optional<Telemetry> store(String externalDeviceId, TelemetryMessage message) {
        Device device = deviceRepository.findByDeviceIdForUpdate(externalDeviceId)
                .orElseThrow(() -> new DeviceNotFoundException(externalDeviceId));
        if (telemetryRepository.existsByDeviceIdAndBootIdAndSequenceNumber(
                device.getId(), message.bootId(), message.sequenceNumber())) {
            return Optional.empty();
        }

        device.observeTelemetry(message.bootId(), message.receivedAt());
        Telemetry telemetry = new Telemetry(
                device,
                message.protocolVersion(),
                message.bootId(),
                message.sequenceNumber(),
                message.recordedAt(),
                message.receivedAt(),
                message.latitude(),
                message.longitude(),
                message.gnssSpeedKph(),
                message.vehicleSpeedKph(),
                message.rpm(),
                message.acceleratorPct(),
                message.rssiDbm());
        tripService.assign(telemetry);
        Telemetry saved = telemetryRepository.save(telemetry);
        vehicleStateService.observe(saved);
        return Optional.of(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<TelemetryResponse> findVehicleHistory(
            long vehicleId, Instant from, Instant to, int page, int size) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new VehicleNotFoundException(vehicleId);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must not be after to");
        }
        var pageable = PageRequest.of(page, size);
        var history = from == null
                ? (to == null
                    ? telemetryRepository.findVehicleHistory(vehicleId, pageable)
                    : telemetryRepository.findVehicleHistoryTo(vehicleId, to, pageable))
                : (to == null
                    ? telemetryRepository.findVehicleHistoryFrom(vehicleId, from, pageable)
                    : telemetryRepository.findVehicleHistoryBetween(vehicleId, from, to, pageable));
        return PageResponse.from(history.map(TelemetryResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<TelemetryResponse> findTripTelemetry(long tripId, int page, int size) {
        if (!tripRepository.existsById(tripId)) {
            throw new TripNotFoundException(tripId);
        }
        return PageResponse.from(telemetryRepository.findByTripIdOrderByRecordedAtAscIdAsc(
                tripId, PageRequest.of(page, size)).map(TelemetryResponse::from));
    }
}

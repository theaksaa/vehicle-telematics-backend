package com.vehicletelematics.backend.telemetry.service;

import com.vehicletelematics.backend.devices.domain.Device;
import com.vehicletelematics.backend.devices.exception.DeviceNotFoundException;
import com.vehicletelematics.backend.devices.repository.DeviceRepository;
import com.vehicletelematics.backend.telemetry.domain.Telemetry;
import com.vehicletelematics.backend.telemetry.repository.TelemetryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class TelemetryService {

    private final DeviceRepository deviceRepository;
    private final TelemetryRepository telemetryRepository;

    public TelemetryService(DeviceRepository deviceRepository, TelemetryRepository telemetryRepository) {
        this.deviceRepository = deviceRepository;
        this.telemetryRepository = telemetryRepository;
    }

    @Transactional
    public Optional<Telemetry> store(String externalDeviceId, TelemetryMessage message) {
        Device device = deviceRepository.findByDeviceId(externalDeviceId)
                .orElseThrow(() -> new DeviceNotFoundException(externalDeviceId));
        if (telemetryRepository.existsByDeviceIdAndBootIdAndSequenceNumber(
                device.getId(), message.bootId(), message.sequenceNumber())) {
            return Optional.empty();
        }

        return Optional.of(telemetryRepository.save(new Telemetry(
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
                message.rssiDbm())));
    }
}

package com.vehicletelematics.backend.telemetry.repository;

import com.vehicletelematics.backend.telemetry.domain.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    boolean existsByDeviceIdAndBootIdAndSequenceNumber(Long deviceId, long bootId, long sequenceNumber);
}

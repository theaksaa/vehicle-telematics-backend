package com.vehicletelematics.backend.telemetry.repository;

import com.vehicletelematics.backend.telemetry.domain.Telemetry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;

public interface TelemetryRepository extends JpaRepository<Telemetry, Long> {
    boolean existsByDeviceIdAndBootIdAndSequenceNumber(Long deviceId, long bootId, long sequenceNumber);

    @Query("""
            select t from Telemetry t
            where t.vehicle.id = :vehicleId
            order by t.recordedAt desc, t.id desc
            """)
    Page<Telemetry> findVehicleHistory(Long vehicleId, Pageable pageable);

    @Query("""
            select t from Telemetry t
            where t.vehicle.id = :vehicleId and t.recordedAt >= :from
            order by t.recordedAt desc, t.id desc
            """)
    Page<Telemetry> findVehicleHistoryFrom(Long vehicleId, Instant from, Pageable pageable);

    @Query("""
            select t from Telemetry t
            where t.vehicle.id = :vehicleId and t.recordedAt <= :to
            order by t.recordedAt desc, t.id desc
            """)
    Page<Telemetry> findVehicleHistoryTo(Long vehicleId, Instant to, Pageable pageable);

    @Query("""
            select t from Telemetry t
            where t.vehicle.id = :vehicleId
              and t.recordedAt >= :from and t.recordedAt <= :to
            order by t.recordedAt desc, t.id desc
            """)
    Page<Telemetry> findVehicleHistoryBetween(Long vehicleId, Instant from, Instant to, Pageable pageable);

    Page<Telemetry> findByTripIdOrderByRecordedAtAscIdAsc(Long tripId, Pageable pageable);
}

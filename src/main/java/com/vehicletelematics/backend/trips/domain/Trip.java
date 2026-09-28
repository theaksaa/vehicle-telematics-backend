package com.vehicletelematics.backend.trips.domain;

import com.vehicletelematics.backend.telemetry.domain.Telemetry;
import com.vehicletelematics.backend.vehicles.domain.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "trips")
@Getter
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "last_telemetry_at", nullable = false)
    private Instant lastTelemetryAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TripStatus status;

    @Column(name = "start_latitude")
    private Double startLatitude;

    @Column(name = "start_longitude")
    private Double startLongitude;

    @Column(name = "end_latitude")
    private Double endLatitude;

    @Column(name = "end_longitude")
    private Double endLongitude;

    @Column(name = "telemetry_count", nullable = false)
    private long telemetryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Trip() {
    }

    public Trip(Vehicle vehicle, Telemetry firstTelemetry) {
        this.vehicle = vehicle;
        this.startedAt = firstTelemetry.getRecordedAt();
        this.lastTelemetryAt = firstTelemetry.getRecordedAt();
        this.status = TripStatus.OPEN;
        observe(firstTelemetry);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void observe(Telemetry telemetry) {
        if (telemetry.getRecordedAt().isAfter(lastTelemetryAt)) {
            lastTelemetryAt = telemetry.getRecordedAt();
        }
        if (telemetry.getLatitude() != null && telemetry.getLongitude() != null) {
            endLatitude = telemetry.getLatitude();
            endLongitude = telemetry.getLongitude();
            if (startLatitude == null || startLongitude == null) {
                startLatitude = telemetry.getLatitude();
                startLongitude = telemetry.getLongitude();
            }
        }
        telemetryCount++;
    }

    public void close(Instant endedAt) {
        if (status == TripStatus.CLOSED) {
            return;
        }
        Instant candidate = endedAt == null ? lastTelemetryAt : endedAt;
        this.endedAt = candidate.isBefore(startedAt) ? lastTelemetryAt : candidate;
        this.status = TripStatus.CLOSED;
    }
}

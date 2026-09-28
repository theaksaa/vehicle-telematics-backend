package com.vehicletelematics.backend.vehicles.state;

import com.vehicletelematics.backend.telemetry.domain.Telemetry;
import com.vehicletelematics.backend.vehicles.domain.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "vehicle_state")
@Getter
public class VehicleState {

    @Id
    @Column(name = "vehicle_id")
    private Long vehicleId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(nullable = false)
    private boolean online;

    @Column(name = "last_telemetry_at")
    private Instant lastTelemetryAt;

    @Column(name = "last_location_at")
    private Instant lastLocationAt;

    private Double latitude;
    private Double longitude;

    @Column(name = "speed_kph")
    private Float speedKph;

    private Integer rpm;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VehicleState() {
    }

    public VehicleState(Vehicle vehicle) {
        this.vehicle = vehicle;
        this.online = false;
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void updateConnectivity(boolean online) {
        this.online = online;
        this.updatedAt = Instant.now();
    }

    public void observe(Telemetry telemetry) {
        online = true;
        Instant recordedAt = telemetry.getRecordedAt();
        if (lastTelemetryAt == null || !recordedAt.isBefore(lastTelemetryAt)) {
            lastTelemetryAt = recordedAt;
            speedKph = telemetry.getVehicleSpeedKph() != null
                    ? telemetry.getVehicleSpeedKph()
                    : telemetry.getGnssSpeedKph();
            rpm = telemetry.getRpm();
        }
        if (telemetry.getLatitude() != null && telemetry.getLongitude() != null
                && (lastLocationAt == null || !recordedAt.isBefore(lastLocationAt))) {
            latitude = telemetry.getLatitude();
            longitude = telemetry.getLongitude();
            lastLocationAt = recordedAt;
        }
        updatedAt = Instant.now();
    }
}

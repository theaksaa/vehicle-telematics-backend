package com.vehicletelematics.backend.telemetry.domain;

import com.vehicletelematics.backend.devices.domain.Device;
import com.vehicletelematics.backend.vehicles.domain.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "telemetry", uniqueConstraints = @UniqueConstraint(
        name = "uk_telemetry_device_boot_sequence",
        columnNames = {"device_id", "boot_id", "sequence_number"}))
@Getter
public class Telemetry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @Column(name = "trip_id")
    private Long tripId;

    @Column(name = "protocol_version", nullable = false)
    private short protocolVersion;

    @Column(name = "boot_id", nullable = false)
    private long bootId;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    private Double latitude;
    private Double longitude;

    @Column(name = "gnss_speed_kph")
    private Float gnssSpeedKph;

    @Column(name = "vehicle_speed_kph")
    private Float vehicleSpeedKph;

    private Integer rpm;

    @Column(name = "accelerator_pct")
    private Float acceleratorPct;

    @Column(name = "rssi_dbm")
    private Short rssiDbm;

    protected Telemetry() {
    }

    public Telemetry(
            Device device,
            short protocolVersion,
            long bootId,
            long sequenceNumber,
            Instant recordedAt,
            Instant receivedAt,
            Double latitude,
            Double longitude,
            Float gnssSpeedKph,
            Float vehicleSpeedKph,
            Integer rpm,
            Float acceleratorPct,
            Short rssiDbm) {
        this.device = device;
        this.vehicle = device.getVehicle();
        this.protocolVersion = protocolVersion;
        this.bootId = bootId;
        this.sequenceNumber = sequenceNumber;
        this.recordedAt = recordedAt;
        this.receivedAt = receivedAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.gnssSpeedKph = gnssSpeedKph;
        this.vehicleSpeedKph = vehicleSpeedKph;
        this.rpm = rpm;
        this.acceleratorPct = acceleratorPct;
        this.rssiDbm = rssiDbm;
    }
}

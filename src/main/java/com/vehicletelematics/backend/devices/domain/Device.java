package com.vehicletelematics.backend.devices.domain;

import tools.jackson.databind.JsonNode;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "devices")
@Getter
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 64)
    private String deviceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", unique = true)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.OFFLINE;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "last_boot_id")
    private Long lastBootId;

    @Column(name = "firmware_version", length = 32)
    private String firmwareVersion;

    @Column(name = "desired_config_version")
    private Long desiredConfigVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "desired_config", columnDefinition = "jsonb")
    private JsonNode desiredConfig;

    @Column(name = "desired_config_updated_at")
    private Instant desiredConfigUpdatedAt;

    @Column(name = "reported_config_version")
    private Long reportedConfigVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reported_config", columnDefinition = "jsonb")
    private JsonNode reportedConfig;

    @Column(name = "reported_config_at")
    private Instant reportedConfigAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Device() {
    }

    public Device(String deviceId, String firmwareVersion) {
        this.deviceId = deviceId;
        this.firmwareVersion = firmwareVersion;
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

    public void update(
            String deviceId,
            DeviceStatus status,
            Instant lastSeenAt,
            Long lastBootId,
            String firmwareVersion) {
        this.deviceId = deviceId;
        this.status = status;
        this.lastSeenAt = lastSeenAt;
        this.lastBootId = lastBootId;
        this.firmwareVersion = firmwareVersion;
    }

    public void updateDesiredConfig(JsonNode config) {
        desiredConfigVersion = desiredConfigVersion == null ? 1 : desiredConfigVersion + 1;
        desiredConfig = config.deepCopy();
        desiredConfigUpdatedAt = Instant.now();
    }

    public void updateReportedConfig(long version, JsonNode config) {
        if (reportedConfigVersion != null && version < reportedConfigVersion) {
            return;
        }
        reportedConfigVersion = version;
        reportedConfig = config.deepCopy();
        reportedConfigAt = Instant.now();
    }

    public DeviceConfigState getConfigState() {
        if (desiredConfigVersion == null || desiredConfig == null) {
            return DeviceConfigState.NOT_CONFIGURED;
        }
        if (reportedConfigVersion == null || !reportedConfigVersion.equals(desiredConfigVersion)) {
            return DeviceConfigState.PENDING;
        }
        return desiredConfig.equals(reportedConfig)
                ? DeviceConfigState.APPLIED
                : DeviceConfigState.DRIFTED;
    }

    public void assignTo(Vehicle vehicle) {
        this.vehicle = vehicle;
        vehicle.assignDevice(this);
    }

    public void unassign() {
        if (vehicle != null) {
            vehicle.removeDevice();
        }
        vehicle = null;
    }
}

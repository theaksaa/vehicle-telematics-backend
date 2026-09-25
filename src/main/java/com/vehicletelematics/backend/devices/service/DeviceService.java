package com.vehicletelematics.backend.devices.service;

import tools.jackson.databind.JsonNode;
import com.vehicletelematics.backend.devices.domain.Device;
import com.vehicletelematics.backend.devices.domain.DeviceStatus;
import com.vehicletelematics.backend.devices.exception.DeviceAlreadyExistsException;
import com.vehicletelematics.backend.devices.exception.DeviceAssignmentConflictException;
import com.vehicletelematics.backend.devices.exception.DeviceNotFoundException;
import com.vehicletelematics.backend.devices.exception.InvalidDeviceDataException;
import com.vehicletelematics.backend.devices.repository.DeviceRepository;
import com.vehicletelematics.backend.vehicles.domain.Vehicle;
import com.vehicletelematics.backend.vehicles.exception.VehicleNotFoundException;
import com.vehicletelematics.backend.vehicles.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class DeviceService {

    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    private final DeviceRepository deviceRepository;
    private final VehicleRepository vehicleRepository;

    public DeviceService(DeviceRepository deviceRepository, VehicleRepository vehicleRepository) {
        this.deviceRepository = deviceRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public List<Device> findAll() {
        return deviceRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Device findByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    @Transactional
    public Device create(String deviceId, String firmwareVersion) {
        String normalizedDeviceId = normalizeDeviceId(deviceId);
        ensureDeviceIdAvailable(normalizedDeviceId, null);
        return deviceRepository.save(new Device(
                normalizedDeviceId,
                normalizeOptional(firmwareVersion, 32, "Firmware version")));
    }

    @Transactional
    public Device update(
            String currentDeviceId,
            String deviceId,
            DeviceStatus status,
            Instant lastSeenAt,
            Long lastBootId,
            String firmwareVersion) {
        Device device = findByDeviceId(currentDeviceId);
        String updatedDeviceId = deviceId == null ? device.getDeviceId() : normalizeDeviceId(deviceId);
        ensureDeviceIdAvailable(updatedDeviceId, device.getId());

        device.update(
                updatedDeviceId,
                status == null ? device.getStatus() : status,
                lastSeenAt == null ? device.getLastSeenAt() : lastSeenAt,
                lastBootId == null ? device.getLastBootId() : validateNonNegative(lastBootId, "Last boot ID"),
                firmwareVersion == null
                        ? device.getFirmwareVersion()
                        : normalizeOptional(firmwareVersion, 32, "Firmware version"));
        return device;
    }

    @Transactional
    public Device updateDesiredConfig(String deviceId, JsonNode payload) {
        validateConfigPayload(payload);
        Device device = findByDeviceIdForUpdate(deviceId);
        device.updateDesiredConfig(payload);
        return device;
    }

    @Transactional
    public Device reportConfig(String deviceId, long version, JsonNode payload) {
        validateConfigPayload(payload);
        Device device = findByDeviceIdForUpdate(deviceId);
        if (device.getDesiredConfigVersion() == null) {
            throw new InvalidDeviceDataException("Device does not have a desired configuration");
        }
        if (version > device.getDesiredConfigVersion()) {
            throw new InvalidDeviceDataException(
                    "Reported config version cannot be newer than desired config version");
        }
        device.updateReportedConfig(version, payload);
        return device;
    }

    @Transactional
    public void delete(String deviceId) {
        Device device = findByDeviceId(deviceId);
        if (device.getVehicle() != null) {
            throw new DeviceAssignmentConflictException("Assigned device must be detached before deletion");
        }
        deviceRepository.delete(device);
    }

    @Transactional
    public Device assign(long vehicleId, String deviceId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
        Device device = findByDeviceId(deviceId);

        if (device.getVehicle() != null && !device.getVehicle().getId().equals(vehicleId)) {
            throw new DeviceAssignmentConflictException(
                    "Device " + deviceId + " is already assigned to another vehicle");
        }
        deviceRepository.findByVehicleId(vehicleId)
                .filter(assigned -> !assigned.getId().equals(device.getId()))
                .ifPresent(assigned -> {
                    throw new DeviceAssignmentConflictException(
                            "Vehicle " + vehicleId + " already has an assigned device");
                });

        device.assignTo(vehicle);
        return device;
    }

    @Transactional
    public void unassign(long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new VehicleNotFoundException(vehicleId);
        }
        deviceRepository.findByVehicleId(vehicleId).ifPresent(Device::unassign);
    }

    private String normalizeDeviceId(String deviceId) {
        if (deviceId == null) {
            throw new InvalidDeviceDataException("Device ID is required");
        }
        String normalized = deviceId.trim();
        if (!DEVICE_ID_PATTERN.matcher(normalized).matches()) {
            throw new InvalidDeviceDataException(
                    "Device ID must contain 1 to 64 letters, numbers, hyphens or underscores");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maximumLength, String fieldName) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maximumLength) {
            throw new InvalidDeviceDataException(fieldName + " must not exceed " + maximumLength + " characters");
        }
        return normalized;
    }

    private Long validateNonNegative(Long value, String fieldName) {
        if (value != null && value < 0) {
            throw new InvalidDeviceDataException(fieldName + " must not be negative");
        }
        return value;
    }

    private void validateConfigPayload(JsonNode payload) {
        if (payload == null || payload.isNull() || !payload.isObject()) {
            throw new InvalidDeviceDataException("Device configuration must be a JSON object");
        }
    }

    private Device findByDeviceIdForUpdate(String deviceId) {
        return deviceRepository.findByDeviceIdForUpdate(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException(deviceId));
    }

    private void ensureDeviceIdAvailable(String deviceId, Long currentId) {
        deviceRepository.findByDeviceId(deviceId)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new DeviceAlreadyExistsException(deviceId);
                });
    }
}

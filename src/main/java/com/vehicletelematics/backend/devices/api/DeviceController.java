package com.vehicletelematics.backend.devices.api;

import tools.jackson.databind.JsonNode;
import com.vehicletelematics.backend.devices.api.dto.CreateDeviceRequest;
import com.vehicletelematics.backend.devices.api.dto.DeviceResponse;
import com.vehicletelematics.backend.devices.api.dto.UpdateDeviceRequest;
import com.vehicletelematics.backend.devices.domain.Device;
import com.vehicletelematics.backend.devices.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceResponse> findAll() {
        return deviceService.findAll().stream().map(DeviceResponse::from).toList();
    }

    @GetMapping("/{deviceId}")
    public DeviceResponse findByDeviceId(@PathVariable String deviceId) {
        return DeviceResponse.from(deviceService.findByDeviceId(deviceId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse create(@Valid @RequestBody CreateDeviceRequest request) {
        Device device = deviceService.create(
                request.deviceId(), request.firmwareVersion());
        return DeviceResponse.from(device);
    }

    @PatchMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public DeviceResponse update(
            @PathVariable String deviceId,
            @Valid @RequestBody UpdateDeviceRequest request) {
        Device device = deviceService.update(
                deviceId,
                request.deviceId(),
                request.status(),
                request.lastSeenAt(),
                request.lastBootId(),
                request.firmwareVersion());
        return DeviceResponse.from(device);
    }

    @PutMapping("/{deviceId}/config/desired")
    @PreAuthorize("hasRole('ADMIN')")
    public DeviceResponse updateDesiredConfig(
            @PathVariable String deviceId,
            @RequestBody JsonNode payload) {
        return DeviceResponse.from(deviceService.updateDesiredConfig(deviceId, payload));
    }

    @DeleteMapping("/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String deviceId) {
        deviceService.delete(deviceId);
    }
}

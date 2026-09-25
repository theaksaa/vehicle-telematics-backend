package com.vehicletelematics.backend.devices.api;

import com.vehicletelematics.backend.devices.api.dto.DeviceResponse;
import com.vehicletelematics.backend.devices.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
@PreAuthorize("hasRole('ADMIN')")
public class DeviceAssignmentController {

    private final DeviceService deviceService;

    public DeviceAssignmentController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PutMapping("/{vehicleId}/device/{deviceId}")
    public DeviceResponse assign(
            @PathVariable long vehicleId,
            @PathVariable String deviceId) {
        return DeviceResponse.from(deviceService.assign(vehicleId, deviceId));
    }

    @DeleteMapping("/{vehicleId}/device")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable long vehicleId) {
        deviceService.unassign(vehicleId);
    }
}

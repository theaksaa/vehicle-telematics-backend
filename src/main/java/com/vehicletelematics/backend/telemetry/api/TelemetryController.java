package com.vehicletelematics.backend.telemetry.api;

import com.vehicletelematics.backend.common.api.PageResponse;
import com.vehicletelematics.backend.telemetry.api.dto.TelemetryResponse;
import com.vehicletelematics.backend.telemetry.service.TelemetryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@Validated
@RequestMapping("/api")
public class TelemetryController {

    private final TelemetryService telemetryService;

    public TelemetryController(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @GetMapping("/vehicles/{vehicleId}/telemetry")
    public PageResponse<TelemetryResponse> vehicleHistory(
            @PathVariable long vehicleId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size) {
        return telemetryService.findVehicleHistory(vehicleId, from, to, page, size);
    }

    @GetMapping("/trips/{tripId}/telemetry")
    public PageResponse<TelemetryResponse> tripTelemetry(
            @PathVariable long tripId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(1000) int size) {
        return telemetryService.findTripTelemetry(tripId, page, size);
    }
}

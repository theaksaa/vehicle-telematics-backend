package com.vehicletelematics.backend.trips.api;

import com.vehicletelematics.backend.common.api.PageResponse;
import com.vehicletelematics.backend.trips.api.dto.TripResponse;
import com.vehicletelematics.backend.trips.service.TripService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api")
public class TripController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    @GetMapping("/vehicles/{vehicleId}/trips")
    public PageResponse<TripResponse> findByVehicle(
            @PathVariable long vehicleId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return tripService.findByVehicle(vehicleId, page, size);
    }

    @GetMapping("/trips/{tripId}")
    public TripResponse find(@PathVariable long tripId) {
        return tripService.find(tripId);
    }
}

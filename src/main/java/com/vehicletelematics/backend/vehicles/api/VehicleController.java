package com.vehicletelematics.backend.vehicles.api;

import com.vehicletelematics.backend.vehicles.api.dto.CreateVehicleRequest;
import com.vehicletelematics.backend.vehicles.api.dto.UpdateVehicleRequest;
import com.vehicletelematics.backend.vehicles.api.dto.VehicleResponse;
import com.vehicletelematics.backend.vehicles.domain.Vehicle;
import com.vehicletelematics.backend.vehicles.service.VehicleService;
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

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<VehicleResponse> findAll() {
        return vehicleService.findAll().stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public VehicleResponse findById(@PathVariable long id) {
        return VehicleResponse.from(vehicleService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse create(@Valid @RequestBody CreateVehicleRequest request) {
        Vehicle vehicle = vehicleService.create(
                request.registration(),
                request.manufacturer(),
                request.model(),
                request.year(),
                request.vin(),
                request.description());
        return VehicleResponse.from(vehicle);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public VehicleResponse update(
            @PathVariable long id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        Vehicle vehicle = vehicleService.update(
                id,
                request.registration(),
                request.manufacturer(),
                request.model(),
                request.year(),
                request.vin(),
                request.description(),
                request.active());
        return VehicleResponse.from(vehicle);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable long id) {
        vehicleService.deactivate(id);
    }
}

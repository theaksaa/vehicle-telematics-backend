package com.vehicletelematics.backend.vehicles.api;

import com.vehicletelematics.backend.vehicles.api.dto.VehicleStateResponse;
import com.vehicletelematics.backend.vehicles.state.VehicleStateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleStateController {

    private final VehicleStateService stateService;

    public VehicleStateController(VehicleStateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping("/{id}/state")
    public VehicleStateResponse findState(@PathVariable long id) {
        return VehicleStateResponse.from(stateService.find(id));
    }
}

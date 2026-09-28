package com.vehicletelematics.backend.vehicles.state;

import com.vehicletelematics.backend.devices.domain.Device;
import com.vehicletelematics.backend.devices.domain.DeviceStatus;
import com.vehicletelematics.backend.telemetry.domain.Telemetry;
import com.vehicletelematics.backend.vehicles.domain.Vehicle;
import com.vehicletelematics.backend.vehicles.exception.VehicleNotFoundException;
import com.vehicletelematics.backend.vehicles.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleStateService {

    private final VehicleStateRepository stateRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleStateService(VehicleStateRepository stateRepository, VehicleRepository vehicleRepository) {
        this.stateRepository = stateRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public VehicleState initialize(Vehicle vehicle) {
        return stateRepository.findById(vehicle.getId())
                .orElseGet(() -> stateRepository.save(new VehicleState(vehicle)));
    }

    @Transactional
    public void updateConnectivity(Device device) {
        if (device.getVehicle() == null) {
            return;
        }
        updateConnectivity(device.getVehicle(), device.getStatus() == DeviceStatus.ONLINE);
    }

    @Transactional
    public void updateConnectivity(Vehicle vehicle, boolean online) {
        initialize(vehicle).updateConnectivity(online);
    }

    @Transactional
    public void observe(Telemetry telemetry) {
        if (telemetry.getVehicle() != null) {
            initialize(telemetry.getVehicle()).observe(telemetry);
        }
    }

    @Transactional(readOnly = true)
    public VehicleState find(long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new VehicleNotFoundException(vehicleId);
        }
        return stateRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalStateException("Vehicle state is not initialized"));
    }
}

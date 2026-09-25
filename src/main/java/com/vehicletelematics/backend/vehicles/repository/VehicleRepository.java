package com.vehicletelematics.backend.vehicles.repository;

import com.vehicletelematics.backend.vehicles.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByRegistration(String registration);

    Optional<Vehicle> findByVin(String vin);

    List<Vehicle> findAllByOrderByIdAsc();
}

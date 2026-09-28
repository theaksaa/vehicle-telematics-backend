package com.vehicletelematics.backend.trips.repository;

import com.vehicletelematics.backend.trips.domain.Trip;
import com.vehicletelematics.backend.trips.domain.TripStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Trip> findFirstByVehicleIdAndStatusOrderByStartedAtDesc(Long vehicleId, TripStatus status);

    Page<Trip> findByVehicleIdOrderByStartedAtDesc(Long vehicleId, Pageable pageable);
}

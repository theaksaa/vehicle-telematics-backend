package com.vehicletelematics.backend.devices.repository;

import com.vehicletelematics.backend.devices.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceId(String deviceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select device from Device device where device.deviceId = :deviceId")
    Optional<Device> findByDeviceIdForUpdate(@Param("deviceId") String deviceId);

    Optional<Device> findByVehicleId(Long vehicleId);

    List<Device> findAllByOrderByIdAsc();
}

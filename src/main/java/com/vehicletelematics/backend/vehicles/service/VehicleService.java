package com.vehicletelematics.backend.vehicles.service;

import com.vehicletelematics.backend.vehicles.domain.Vehicle;
import com.vehicletelematics.backend.vehicles.exception.InvalidVehicleDataException;
import com.vehicletelematics.backend.vehicles.exception.InvalidVehicleYearException;
import com.vehicletelematics.backend.vehicles.exception.RegistrationAlreadyExistsException;
import com.vehicletelematics.backend.vehicles.exception.VehicleNotFoundException;
import com.vehicletelematics.backend.vehicles.exception.VinAlreadyExistsException;
import com.vehicletelematics.backend.vehicles.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.Locale;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> findAll() {
        return vehicleRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public Vehicle findById(long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    @Transactional
    public Vehicle create(
            String registration,
            String manufacturer,
            String model,
            Short year,
            String vin,
            String description) {
        String normalizedRegistration = normalizeRegistration(registration);
        String normalizedManufacturer = normalizeRequiredText(manufacturer, "Manufacturer", 64);
        String normalizedModel = normalizeRequiredText(model, "Model", 64);
        String normalizedVin = normalizeOptional(vin, 17, "VIN", true);
        String normalizedDescription = normalizeOptional(description, 255, "Description", false);

        validateRegistration(normalizedRegistration);
        validateYear(year);
        ensureRegistrationAvailable(normalizedRegistration, null);
        ensureVinAvailable(normalizedVin, null);

        return vehicleRepository.save(new Vehicle(
                normalizedRegistration,
                normalizedManufacturer,
                normalizedModel,
                year,
                normalizedVin,
                normalizedDescription));
    }

    @Transactional
    public Vehicle update(
            long id,
            String registration,
            String manufacturer,
            String model,
            Short year,
            String vin,
            String description,
            Boolean active) {
        Vehicle vehicle = findById(id);

        String updatedRegistration = registration == null
                ? vehicle.getRegistration()
                : normalizeRegistration(registration);
        String updatedManufacturer = manufacturer == null
                ? vehicle.getManufacturer()
                : normalizeRequiredText(manufacturer, "Manufacturer", 64);
        String updatedModel = model == null
                ? vehicle.getModel()
                : normalizeRequiredText(model, "Model", 64);
        Short updatedYear = year == null ? vehicle.getYear() : year;
        String updatedVin = vin == null ? vehicle.getVin() : normalizeOptional(vin, 17, "VIN", true);
        String updatedDescription = description == null
                ? vehicle.getDescription()
                : normalizeOptional(description, 255, "Description", false);

        validateRegistration(updatedRegistration);
        validateYear(updatedYear);
        ensureRegistrationAvailable(updatedRegistration, vehicle.getId());
        ensureVinAvailable(updatedVin, vehicle.getId());

        vehicle.update(
                updatedRegistration,
                updatedManufacturer,
                updatedModel,
                updatedYear,
                updatedVin,
                updatedDescription,
                active == null ? vehicle.isActive() : active);
        return vehicle;
    }

    @Transactional
    public void deactivate(long id) {
        findById(id).deactivate();
    }

    private String normalizeRegistration(String registration) {
        if (registration == null) {
            throw new InvalidVehicleDataException("Registration is required");
        }
        return registration.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRequiredText(String value, String fieldName, int maximumLength) {
        if (value == null) {
            throw new InvalidVehicleDataException(fieldName + " is required");
        }
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > maximumLength) {
            throw new InvalidVehicleDataException(
                    fieldName + " must contain between 1 and " + maximumLength + " characters");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maximumLength, String fieldName, boolean uppercase) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maximumLength) {
            throw new InvalidVehicleDataException(fieldName + " must not exceed " + maximumLength + " characters");
        }
        return uppercase ? normalized.toUpperCase(Locale.ROOT) : normalized;
    }

    private void validateRegistration(String registration) {
        if (registration.length() < 2 || registration.length() > 20) {
            throw new InvalidVehicleDataException("Registration must contain between 2 and 20 characters");
        }
    }

    private void validateYear(Short year) {
        int maximumYear = Year.now().getValue() + 1;
        if (year != null && (year < 1900 || year > maximumYear)) {
            throw new InvalidVehicleYearException(maximumYear);
        }
    }

    private void ensureRegistrationAvailable(String registration, Long currentVehicleId) {
        vehicleRepository.findByRegistration(registration)
                .filter(existing -> !existing.getId().equals(currentVehicleId))
                .ifPresent(existing -> {
                    throw new RegistrationAlreadyExistsException(registration);
                });
    }

    private void ensureVinAvailable(String vin, Long currentVehicleId) {
        if (vin == null) {
            return;
        }
        vehicleRepository.findByVin(vin)
                .filter(existing -> !existing.getId().equals(currentVehicleId))
                .ifPresent(existing -> {
                    throw new VinAlreadyExistsException(vin);
                });
    }
}

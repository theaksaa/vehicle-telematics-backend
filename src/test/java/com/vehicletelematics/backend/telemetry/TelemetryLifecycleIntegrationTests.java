package com.vehicletelematics.backend.telemetry;

import com.vehicletelematics.backend.devices.domain.DeviceStatus;
import com.vehicletelematics.backend.devices.service.DeviceService;
import com.vehicletelematics.backend.telemetry.service.TelemetryMessage;
import com.vehicletelematics.backend.telemetry.service.TelemetryService;
import com.vehicletelematics.backend.trips.domain.TripStatus;
import com.vehicletelematics.backend.trips.repository.TripRepository;
import com.vehicletelematics.backend.vehicles.domain.Vehicle;
import com.vehicletelematics.backend.vehicles.service.VehicleService;
import com.vehicletelematics.backend.vehicles.state.VehicleStateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TelemetryLifecycleIntegrationTests {

    @Autowired
    private TelemetryService telemetryService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private VehicleStateRepository stateRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void telemetryUpdatesDeviceAndVehicleStateCreatesTripAndExposesHistory() throws Exception {
        Vehicle vehicle = createAssignedVehicle("KG-LIVE-01", "lifecycle-device-01");
        Instant start = Instant.parse("2026-09-27T16:31:00Z");

        telemetryService.store("lifecycle-device-01", message(
                91, 1, start, 44.0123, 20.9123, 32f, 1900));
        telemetryService.store("lifecycle-device-01", message(
                91, 2, start.plusSeconds(30), null, null, 45f, 2200));
        telemetryService.store("lifecycle-device-01", message(
                91, 3, start.plusSeconds(60), null, null, 0f, 0));

        var device = deviceService.findByDeviceId("lifecycle-device-01");
        assertThat(device.getStatus()).isEqualTo(DeviceStatus.ONLINE);
        assertThat(device.getLastBootId()).isEqualTo(91L);
        assertThat(device.getLastSeenAt()).isNotNull();

        var state = stateRepository.findById(vehicle.getId()).orElseThrow();
        assertThat(state.isOnline()).isTrue();
        assertThat(state.getLatitude()).isEqualTo(44.0123);
        assertThat(state.getLongitude()).isEqualTo(20.9123);
        assertThat(state.getLastLocationAt()).isEqualTo(start);
        assertThat(state.getLastTelemetryAt()).isEqualTo(start.plusSeconds(60));
        assertThat(state.getSpeedKph()).isZero();
        assertThat(state.getRpm()).isZero();

        var trip = tripRepository.findAll().getFirst();
        assertThat(trip.getStatus()).isEqualTo(TripStatus.CLOSED);
        assertThat(trip.getStartedAt()).isEqualTo(start);
        assertThat(trip.getEndedAt()).isEqualTo(start.plusSeconds(60));
        assertThat(trip.getTelemetryCount()).isEqualTo(3);

        mockMvc.perform(get("/api/vehicles/{id}/state", vehicle.getId()).with(regularUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.online").value(true))
                .andExpect(jsonPath("$.latitude").value(44.0123))
                .andExpect(jsonPath("$.speedKph").value(0));

        mockMvc.perform(get("/api/vehicles/{id}/telemetry", vehicle.getId()).with(regularUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].sequenceNumber").value(3));

        mockMvc.perform(get("/api/vehicles/{id}/trips", vehicle.getId()).with(regularUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CLOSED"))
                .andExpect(jsonPath("$.content[0].telemetryCount").value(3));

        mockMvc.perform(get("/api/trips/{id}/telemetry", trip.getId()).with(regularUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].sequenceNumber").value(1));
    }

    @Test
    void offlineStateClosesOpenTripAndMarksVehicleOffline() {
        Vehicle vehicle = createAssignedVehicle("KG-LIVE-02", "lifecycle-device-02");
        Instant start = Instant.parse("2026-09-27T17:00:00Z");
        telemetryService.store("lifecycle-device-02", message(4, 1, start, 44.0, 20.0, 20f, 1500));

        deviceService.reportState("lifecycle-device-02", DeviceStatus.OFFLINE, null, start.plusSeconds(30));

        assertThat(stateRepository.findById(vehicle.getId()).orElseThrow().isOnline()).isFalse();
        assertThat(tripRepository.findAll().getFirst().getStatus()).isEqualTo(TripStatus.CLOSED);
        assertThat(tripRepository.findAll().getFirst().getEndedAt()).isEqualTo(start);
    }

    @Test
    void telemetryGapClosesPreviousTripAndStartsAnother() {
        createAssignedVehicle("KG-LIVE-03", "lifecycle-device-03");
        Instant start = Instant.parse("2026-09-27T18:00:00Z");
        telemetryService.store("lifecycle-device-03", message(5, 1, start, 44.0, 20.0, 25f, 1700));
        telemetryService.store("lifecycle-device-03", message(
                5, 2, start.plusSeconds(301), 44.1, 20.1, 30f, 1800));

        assertThat(tripRepository.findAll()).hasSize(2);
        assertThat(tripRepository.findAll().stream().filter(t -> t.getStatus() == TripStatus.CLOSED)).hasSize(1);
        assertThat(tripRepository.findAll().stream().filter(t -> t.getStatus() == TripStatus.OPEN)).hasSize(1);
    }

    private Vehicle createAssignedVehicle(String registration, String deviceId) {
        Vehicle vehicle = vehicleService.create(registration, "Volkswagen", "Golf VI", (short) 2012, null, null);
        deviceService.create(deviceId, null);
        deviceService.assign(vehicle.getId(), deviceId);
        return vehicle;
    }

    private TelemetryMessage message(
            long bootId,
            long sequence,
            Instant recordedAt,
            Double latitude,
            Double longitude,
            Float speed,
            Integer rpm) {
        return new TelemetryMessage(
                (short) 1, bootId, sequence, recordedAt, Instant.now(), latitude, longitude,
                speed, speed, rpm, 10f, (short) -60);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor regularUser() {
        return user("user@example.com").roles("USER");
    }
}

package com.vehicletelematics.backend.devices;

import com.jayway.jsonpath.JsonPath;
import com.vehicletelematics.backend.devices.repository.DeviceRepository;
import com.vehicletelematics.backend.devices.service.DeviceService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.node.JsonNodeFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeviceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private DeviceService deviceService;

    @Test
    void adminCanManageDeviceAndAssignItToVehicle() throws Exception {
        long vehicleId = createVehicle("KG-DEV-01", "Volkswagen", "Golf VI");

        mockMvc.perform(post("/api/devices")
                        .with(admin())
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceId": " device-001 ",
                                  "firmwareVersion": " 1.2.3 "
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deviceId").value("device-001"))
                .andExpect(jsonPath("$.status").value("OFFLINE"))
                .andExpect(jsonPath("$.configState").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.vehicleId").doesNotExist());

        mockMvc.perform(patch("/api/devices/device-001")
                        .with(admin())
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "ONLINE",
                                  "lastBootId": 12,
                                  "firmwareVersion": "1.2.4"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.lastBootId").value(12));

        mockMvc.perform(put("/api/vehicles/{vehicleId}/device/{deviceId}", vehicleId, "device-001")
                        .with(admin())
                        .with(realCsrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleId").value(vehicleId));

        mockMvc.perform(get("/api/vehicles/{vehicleId}", vehicleId).with(regularUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("Golf VI"))
                .andExpect(jsonPath("$.deviceId").value("device-001"));

        mockMvc.perform(delete("/api/devices/device-001").with(admin()).with(realCsrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/vehicles/{vehicleId}/device", vehicleId)
                        .with(admin())
                        .with(realCsrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/devices/device-001").with(admin()).with(realCsrf()))
                .andExpect(status().isNoContent());

        assertThat(deviceRepository.findByDeviceId("device-001")).isEmpty();
    }

    @Test
    void assignmentEnforcesOneDevicePerVehicleAndOneVehiclePerDevice() throws Exception {
        long firstVehicleId = createVehicle("KG-DEV-02", "Volkswagen", "Golf VI");
        long secondVehicleId = createVehicle("KG-DEV-03", "Skoda", "Octavia");
        createDevice("device-002");
        createDevice("device-003");

        assign(firstVehicleId, "device-002").andExpect(status().isOk());
        assign(firstVehicleId, "device-003").andExpect(status().isConflict());
        assign(secondVehicleId, "device-002").andExpect(status().isConflict());
    }

    @Test
    void validatesDeviceIdUniquenessAndAdminOnlyMutation() throws Exception {
        createDevice("device_valid-04");

        mockMvc.perform(post("/api/devices")
                        .with(admin())
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"device valid 04\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/devices")
                        .with(admin())
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"device_valid-04\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/devices")
                        .with(regularUser())
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"device-005\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/devices").with(regularUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.deviceId == 'device_valid-04')]").exists());
    }

    @Test
    void desiredConfigVersionIncrementsAutomaticallyAndReportedConfigTracksApplication() throws Exception {
        createDevice("device-config-01");

        mockMvc.perform(put("/api/devices/device-config-01/config/desired")
                        .with(admin())
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "samplingIntervalSeconds": 30,
                                  "gpsEnabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desiredConfigVersion").value(1))
                .andExpect(jsonPath("$.desiredConfig.samplingIntervalSeconds").value(30))
                .andExpect(jsonPath("$.configState").value("PENDING"));

        mockMvc.perform(put("/api/devices/device-config-01/config/desired")
                        .with(admin())
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "samplingIntervalSeconds": 10,
                                  "gpsEnabled": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.desiredConfigVersion").value(2))
                .andExpect(jsonPath("$.configState").value("PENDING"));

        deviceService.reportConfig("device-config-01", 1, configPayload(30));
        assertConfigState("PENDING", 1, 30);

        deviceService.reportConfig("device-config-01", 2, configPayload(10));
        assertConfigState("APPLIED", 2, 10);

        deviceService.reportConfig("device-config-01", 1, configPayload(30));
        assertConfigState("APPLIED", 2, 10);

        deviceService.reportConfig("device-config-01", 2, configPayload(15));
        assertConfigState("DRIFTED", 2, 15);
    }

    private long createVehicle(String registration, String manufacturer, String model) throws Exception {
        String response = mockMvc.perform(post("/api/vehicles")
                        .with(admin())
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "registration": "%s",
                                  "manufacturer": "%s",
                                  "model": "%s"
                                }
                                """.formatted(registration, manufacturer, model)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private void createDevice(String deviceId) throws Exception {
        mockMvc.perform(post("/api/devices")
                        .with(admin())
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"deviceId\":\"%s\"}".formatted(deviceId)))
                .andExpect(status().isCreated());
    }

    private org.springframework.test.web.servlet.ResultActions assign(long vehicleId, String deviceId) throws Exception {
        return mockMvc.perform(put("/api/vehicles/{vehicleId}/device/{deviceId}", vehicleId, deviceId)
                .with(admin())
                .with(realCsrf()));
    }

    private tools.jackson.databind.JsonNode configPayload(int interval) {
        return JsonNodeFactory.instance.objectNode()
                .put("samplingIntervalSeconds", interval)
                .put("gpsEnabled", true);
    }

    private void assertConfigState(String state, long version, int interval) throws Exception {
        mockMvc.perform(get("/api/devices/device-config-01").with(regularUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportedConfigVersion").value(version))
                .andExpect(jsonPath("$.reportedConfig.samplingIntervalSeconds").value(interval))
                .andExpect(jsonPath("$.configState").value(state));
    }

    private RequestPostProcessor admin() {
        return user("admin@example.com").roles("ADMIN");
    }

    private RequestPostProcessor regularUser() {
        return user("user@example.com").roles("USER");
    }

    private RequestPostProcessor realCsrf() throws Exception {
        var response = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();
        String token = JsonPath.read(response.getContentAsString(), "$.token");
        Cookie cookie = response.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return request -> {
            request.addHeader("X-XSRF-TOKEN", token);
            request.setCookies(cookie);
            return request;
        };
    }
}

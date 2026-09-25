package com.vehicletelematics.backend.vehicles;

import com.jayway.jsonpath.JsonPath;
import com.vehicletelematics.backend.vehicles.domain.Vehicle;
import com.vehicletelematics.backend.vehicles.repository.VehicleRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VehicleIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Test
    void adminCanCreateUpdateListAndDeactivateVehicle() throws Exception {
        String location = createVehicle(" kg-123-aa ", "  Volkswagen ", " Golf VI  ", " wvwzzz1jzxw000001 ");
        Vehicle created = vehicleRepository.findByRegistration("KG-123-AA").orElseThrow();

        assertThat(created.getManufacturer()).isEqualTo("Volkswagen");
        assertThat(created.getModel()).isEqualTo("Golf VI");
        assertThat(created.getVin()).isEqualTo("WVWZZZ1JZXW000001");
        assertThat(created.isActive()).isTrue();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
        assertThat(location).contains("KG-123-AA");

        mockMvc.perform(get("/api/vehicles/{id}", created.getId())
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registration").value("KG-123-AA"))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(patch("/api/vehicles/{id}", created.getId())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "manufacturer": "Volkswagen AG",
                                  "description": "Fleet vehicle",
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.manufacturer").value("Volkswagen AG"))
                .andExpect(jsonPath("$.description").value("Fleet vehicle"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/vehicles")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.registration == 'KG-123-AA')]").exists());

        mockMvc.perform(patch("/api/vehicles/{id}", created.getId())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("{\"active\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(delete("/api/vehicles/{id}", created.getId())
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(realCsrf()))
                .andExpect(status().isNoContent());

        assertThat(vehicleRepository.findById(created.getId()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void regularUserCanReadButCannotMutateVehicles() throws Exception {
        createVehicle("BG-001-AA", "Skoda", "Octavia", null);

        mockMvc.perform(get("/api/vehicles")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vehicles")
                        .with(user("user@example.com").roles("USER"))
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "registration": "NS-002-BB",
                                  "manufacturer": "Toyota",
                                  "model": "Corolla"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationAndVinReturnConflict() throws Exception {
        createVehicle("NI-111-AA", "Renault", "Clio", "VF1AAAAAA12345678");

        mockMvc.perform(post("/api/vehicles")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "registration": " ni-111-aa ",
                                  "manufacturer": "Peugeot",
                                  "model": "308"
                                }
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/vehicles")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "registration": "NI-222-BB",
                                  "manufacturer": "Peugeot",
                                  "model": "308",
                                  "vin": "vf1aaaaaa12345678"
                                }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRequiredFieldsAndYearReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/vehicles")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "registration": " X ",
                                  "manufacturer": "Volkswagen",
                                  "model": "Golf"
                                }
                                """))
                .andExpect(status().isBadRequest());

        int invalidYear = Year.now().getValue() + 2;
        mockMvc.perform(post("/api/vehicles")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "registration": "SU-123-AA",
                                  "manufacturer": "Volkswagen",
                                  "model": "Golf",
                                  "year": %d
                                }
                                """.formatted(invalidYear)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingVehicleReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/vehicles/{id}", Long.MAX_VALUE)
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isNotFound());
    }

    private String createVehicle(String registration, String manufacturer, String model, String vin) throws Exception {
        String vinProperty = vin == null ? "" : ", \"vin\": \"" + vin + "\"";
        return mockMvc.perform(post("/api/vehicles")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .with(realCsrf())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "registration": "%s",
                                  "manufacturer": "%s",
                                  "model": "%s",
                                  "year": 2020%s
                                }
                                """.formatted(registration, manufacturer, model, vinProperty)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
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

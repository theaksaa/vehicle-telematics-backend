package com.vehicletelematics.backend.geocoding;

import com.vehicletelematics.backend.geocoding.api.dto.ReverseGeocodingResponse;
import com.vehicletelematics.backend.geocoding.client.ReverseGeocoder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(GeocodingIntegrationTests.GeocodingTestConfiguration.class)
class GeocodingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticatedUserCanReverseGeocode() throws Exception {
        mockMvc.perform(get("/api/geocoding/reverse")
                        .param("lat", "44.8125")
                        .param("lon", "20.4612")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latitude").value(44.8125))
                .andExpect(jsonPath("$.longitude").value(20.4612))
                .andExpect(jsonPath("$.displayName").value("11, Nemanjina, Beograd, Srbija"))
                .andExpect(jsonPath("$.address.road").value("Nemanjina"))
                .andExpect(jsonPath("$.address.city").value("Beograd"))
                .andExpect(jsonPath("$.address.countryCode").value("rs"));
    }

    @Test
    void unauthenticatedUserCannotReverseGeocode() throws Exception {
        mockMvc.perform(get("/api/geocoding/reverse")
                        .param("lat", "44.8125")
                        .param("lon", "20.4612"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void coordinatesOutsideValidRangeAreRejected() throws Exception {
        mockMvc.perform(get("/api/geocoding/reverse")
                        .param("lat", "91")
                        .param("lon", "20.4612")
                        .with(user("user@example.com").roles("USER")))
                .andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class GeocodingTestConfiguration {

        @Bean
        @Primary
        ReverseGeocoder reverseGeocoder() {
            return (latitude, longitude) -> {
                var address = new ReverseGeocodingResponse.Address(
                        "11", "Nemanjina", null, "Savski venac", "Beograd",
                        null, null, "Centralna Srbija", "11000", "Srbija", "rs");
                return new ReverseGeocodingResponse(
                        latitude, longitude, "11, Nemanjina, Beograd, Srbija", address);
            };
        }
    }
}

package com.vehicletelematics.backend.geocoding;

import com.vehicletelematics.backend.geocoding.client.NominatimClient;
import com.vehicletelematics.backend.geocoding.exception.LocationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NominatimClientTests {

    private MockRestServiceServer server;
    private NominatimClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NominatimClient(builder.baseUrl("http://nominatim.test").build());
    }

    @Test
    void mapsNominatimResponseToStableApiResponse() {
        server.expect(requestTo("http://nominatim.test/reverse?lat=44.8125&lon=20.4612"
                        + "&format=jsonv2&addressdetails=1&layer=address"))
                .andRespond(withSuccess("""
                        {
                          "display_name": "11, Nemanjina, Beograd, Srbija",
                          "address": {
                            "house_number": "11",
                            "road": "Nemanjina",
                            "suburb": "Savski venac",
                            "city": "Beograd",
                            "postcode": "11000",
                            "country": "Srbija",
                            "country_code": "rs"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var response = client.reverse(44.8125, 20.4612);

        assertThat(response.displayName()).isEqualTo("11, Nemanjina, Beograd, Srbija");
        assertThat(response.address().road()).isEqualTo("Nemanjina");
        assertThat(response.address().city()).isEqualTo("Beograd");
        assertThat(response.address().countryCode()).isEqualTo("rs");
        server.verify();
    }

    @Test
    void reportsMissingLocation() {
        server.expect(requestTo("http://nominatim.test/reverse?lat=44.0&lon=20.0"
                        + "&format=jsonv2&addressdetails=1&layer=address"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.reverse(44.0, 20.0))
                .isInstanceOf(LocationNotFoundException.class);
        server.verify();
    }
}

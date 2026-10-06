package com.vehicletelematics.backend.geocoding.client;

import com.vehicletelematics.backend.geocoding.api.dto.ReverseGeocodingResponse;
import com.vehicletelematics.backend.geocoding.exception.GeocodingUnavailableException;
import com.vehicletelematics.backend.geocoding.exception.LocationNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class NominatimClient implements ReverseGeocoder {

    private final RestClient restClient;

    public NominatimClient(@Qualifier("nominatimRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public ReverseGeocodingResponse reverse(double latitude, double longitude) {
        try {
            JsonNode result = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("format", "jsonv2")
                            .queryParam("addressdetails", 1)
                            .queryParam("layer", "address")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (result == null || result.get("error") != null) {
                throw new LocationNotFoundException();
            }

            JsonNode address = result.get("address");
            if (address == null || !address.isObject()) {
                throw new LocationNotFoundException();
            }

            return new ReverseGeocodingResponse(
                    latitude,
                    longitude,
                    text(result, "display_name"),
                    new ReverseGeocodingResponse.Address(
                            text(address, "house_number"),
                            text(address, "road"),
                            text(address, "neighbourhood"),
                            text(address, "suburb"),
                            firstText(address, "city", "town", "village", "hamlet"),
                            text(address, "municipality"),
                            text(address, "county"),
                            text(address, "state"),
                            text(address, "postcode"),
                            text(address, "country"),
                            text(address, "country_code")));
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new LocationNotFoundException();
            }
            throw new GeocodingUnavailableException(exception);
        } catch (RestClientException exception) {
            throw new GeocodingUnavailableException(exception);
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}

package com.vehicletelematics.backend.geocoding.api.dto;

public record ReverseGeocodingResponse(
        double latitude,
        double longitude,
        String displayName,
        Address address) {

    public record Address(
            String houseNumber,
            String road,
            String neighbourhood,
            String suburb,
            String city,
            String municipality,
            String county,
            String state,
            String postcode,
            String country,
            String countryCode) {
    }
}

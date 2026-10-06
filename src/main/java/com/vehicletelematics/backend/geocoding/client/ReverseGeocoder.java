package com.vehicletelematics.backend.geocoding.client;

import com.vehicletelematics.backend.geocoding.api.dto.ReverseGeocodingResponse;

public interface ReverseGeocoder {

    ReverseGeocodingResponse reverse(double latitude, double longitude);
}

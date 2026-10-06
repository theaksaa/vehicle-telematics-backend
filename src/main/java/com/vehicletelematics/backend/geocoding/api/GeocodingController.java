package com.vehicletelematics.backend.geocoding.api;

import com.vehicletelematics.backend.geocoding.api.dto.ReverseGeocodingResponse;
import com.vehicletelematics.backend.geocoding.client.ReverseGeocoder;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/geocoding")
public class GeocodingController {

    private final ReverseGeocoder reverseGeocoder;

    public GeocodingController(ReverseGeocoder reverseGeocoder) {
        this.reverseGeocoder = reverseGeocoder;
    }

    @GetMapping("/reverse")
    public ReverseGeocodingResponse reverse(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon) {
        return reverseGeocoder.reverse(lat, lon);
    }
}

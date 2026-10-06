package com.vehicletelematics.backend.geocoding.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class GeocodingConfiguration {

    @Bean
    RestClient nominatimRestClient(
            @Value("${app.geocoding.nominatim.base-url:http://localhost:8088}") String baseUrl,
            @Value("${app.geocoding.nominatim.connect-timeout:2s}") Duration connectTimeout,
            @Value("${app.geocoding.nominatim.read-timeout:3s}") Duration readTimeout,
            @Value("${app.geocoding.nominatim.accept-language:sr-Latn,sr,en;q=0.8}") String acceptLanguage) {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage)
                .defaultHeader(HttpHeaders.USER_AGENT, "vehicle-telematics-backend")
                .build();
    }
}

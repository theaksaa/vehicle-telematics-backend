package com.vehicletelematics.backend.users.api.dto;

public record CsrfResponse(String headerName, String token) {
}

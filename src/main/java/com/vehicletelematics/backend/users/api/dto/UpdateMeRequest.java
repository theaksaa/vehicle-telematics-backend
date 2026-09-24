package com.vehicletelematics.backend.users.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMeRequest(
        @Email @Size(max = 320) @Pattern(regexp = ".*\\S.*") String email,
        @Size(max = 100) @Pattern(regexp = ".*\\S.*") String firstName,
        @Size(max = 100) @Pattern(regexp = ".*\\S.*") String lastName) {
}

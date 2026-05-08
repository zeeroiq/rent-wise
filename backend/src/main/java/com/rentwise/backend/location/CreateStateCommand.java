package com.rentwise.backend.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateStateCommand(
        @NotNull(message = "Country ID is required")
        Long countryId,

        @NotBlank(message = "State code is required")
        String code,

        @NotBlank(message = "State name is required")
        String name
) {
}


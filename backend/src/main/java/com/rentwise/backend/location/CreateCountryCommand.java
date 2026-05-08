package com.rentwise.backend.location;

import jakarta.validation.constraints.NotBlank;

public record CreateCountryCommand(
        @NotBlank(message = "Country code is required")
        String code,

        @NotBlank(message = "Country name is required")
        String name
) {
}


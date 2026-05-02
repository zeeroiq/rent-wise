package com.rentwise.backend.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCityCommand(
        @NotNull(message = "State ID is required")
        Long stateId,

        @NotBlank(message = "City code is required")
        String code,

        @NotBlank(message = "City name is required")
        String name
) {
}


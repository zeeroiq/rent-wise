package com.rentwise.backend.location;

import java.time.LocalDateTime;

public record CityDto(
        Long id,
        Long stateId,
        String code,
        String name,
        LocalDateTime createdAt
) {
}


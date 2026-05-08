package com.rentwise.backend.location;

import java.time.LocalDateTime;

public record StateDto(
        Long id,
        Long countryId,
        String code,
        String name,
        LocalDateTime createdAt
) {
}


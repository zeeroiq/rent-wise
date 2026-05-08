package com.rentwise.backend.location;

import java.time.LocalDateTime;

public record CountryDto(
        Long id,
        String code,
        String name,
        LocalDateTime createdAt
) {
}


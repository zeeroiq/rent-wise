package com.rentwise.backend.web;

import com.rentwise.backend.property.PropertyStatus;
import java.time.LocalDate;

public record PropertyCardDto(
        Long id,
        String title,
        String propertyType,
        String addressLine1,
        String locality,
        String city,
        String state,
        String postalCode,
        String highlights,
        String landlordName,
        PropertyStatus status,
        LocalDate onboardingDate,
        ScorecardDto scorecard
) {
}

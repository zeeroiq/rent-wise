package com.rentwise.backend.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyOnboardingCommand(
        String title,
        String propertyType,
        String addressLine1,
        String locality,
        String city,
        String state,
        String postalCode,
        String highlights,
        Long landlordId,
        LocalDate onboardingDate,
        LocalDate exitDate,
        BigDecimal monthlyRent,
        BigDecimal depositAmount,
        com.rentwise.backend.property.PropertyCondition propertyConditionOnEntry,
        com.rentwise.backend.property.PropertyCondition propertyConditionOnExit,
        String amenities,
        com.rentwise.backend.property.FurnishingType furnishingType,
        com.rentwise.backend.property.OccupancyType occupancyType
) {
}

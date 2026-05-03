package com.rentwise.backend.property;

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
    PropertyCondition propertyConditionOnEntry,
    PropertyCondition propertyConditionOnExit,
    String amenities,
    FurnishingType furnishingType,
    OccupancyType occupancyType
) {
}

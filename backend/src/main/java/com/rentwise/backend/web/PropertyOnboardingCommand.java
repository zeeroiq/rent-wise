package com.rentwise.backend.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyOnboardingCommand(
        @NotBlank String title,
        @NotBlank String propertyType,
        @NotBlank String addressLine1,
        @NotBlank String locality,
        @NotBlank String city,
        @NotBlank String state,
        String postalCode,
        String highlights,
        Long landlordId,
        String landlordName,
        String landlordEmail,
        String landlordPhoneNumber,
        String landlordManagementStyle,
        @NotNull LocalDate onboardingDate,
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

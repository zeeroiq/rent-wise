package com.rentwise.backend.web;

import com.rentwise.backend.property.FurnishingType;
import com.rentwise.backend.property.OccupancyType;
import com.rentwise.backend.property.PropertyCondition;
import com.rentwise.backend.property.PropertyStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PropertyDetailDto(
        Long id,
        String title,
        String propertyType,
        String addressLine1,
        String locality,
        String city,
        String state,
        String postalCode,
        String highlights,
        LocalDate onboardingDate,
        LocalDate exitDate,
        BigDecimal monthlyRent,
        BigDecimal depositAmount,
        PropertyCondition propertyConditionOnEntry,
        PropertyCondition propertyConditionOnExit,
        String amenities,
        FurnishingType furnishingType,
        OccupancyType occupancyType,
        PropertyStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LandlordDto landlord,
        ScorecardDto scorecard,
        List<ReviewDto> reviews,
        SessionUserDto createdBy,
        SessionUserDto verifiedBy,
        LocalDateTime verifiedAt
) {
}

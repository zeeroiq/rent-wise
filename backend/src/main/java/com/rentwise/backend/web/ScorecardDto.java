package com.rentwise.backend.web;

public record ScorecardDto(
        int overallScore,
        int landlordScore,
        int propertyScore,
        String recommendation,
        int reviewCount,
        int unresolvedIssueCount,
        int depositDisputeCount
) {
}

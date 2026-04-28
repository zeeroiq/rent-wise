package com.rentwise.backend.web;

import com.rentwise.backend.review.ReviewVoteType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

record LandlordDto(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String managementStyle
) {
}

record ScorecardDto(
        int overallScore,
        int landlordScore,
        int propertyScore,
        String recommendation,
        int reviewCount,
        int unresolvedIssueCount,
        int depositDisputeCount
) {
}

record PropertyCardDto(
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
        ScorecardDto scorecard
) {
}

record VoteSummaryDto(
        long helpful,
        long notHelpful,
        long sameIssue,
        ReviewVoteType currentUserVote
) {
}

record ReviewCommentDto(
        Long id,
        SessionUserDto author,
        String body,
        LocalDateTime createdAt,
        List<ReviewCommentDto> replies
) {
}

record ReviewDto(
        Long id,
        SessionUserDto author,
        String headline,
        String experienceSummary,
        String problemsFaced,
        String landlordSupport,
        String leaseClosure,
        String securityDepositOutcome,
        int overallRating,
        int landlordRating,
        int propertyRating,
        int maintenanceRating,
        int moveOutRating,
        int depositRating,
        boolean recommended,
        boolean issuesResolved,
        boolean wouldRentAgain,
        LocalDateTime createdAt,
        VoteSummaryDto votes,
        List<ReviewCommentDto> thread
) {
}

record PropertyDetailDto(
        Long id,
        String title,
        String propertyType,
        String addressLine1,
        String locality,
        String city,
        String state,
        String postalCode,
        String highlights,
        LandlordDto landlord,
        ScorecardDto scorecard,
        List<ReviewDto> reviews
) {
}

record CreateReviewCommand(
        @NotBlank String headline,
        @NotBlank String experienceSummary,
        @NotBlank String problemsFaced,
        @NotBlank String landlordSupport,
        @NotBlank String leaseClosure,
        @NotBlank String securityDepositOutcome,
        @Min(1) @Max(5) int overallRating,
        @Min(1) @Max(5) int landlordRating,
        @Min(1) @Max(5) int propertyRating,
        @Min(1) @Max(5) int maintenanceRating,
        @Min(1) @Max(5) int moveOutRating,
        @Min(1) @Max(5) int depositRating,
        boolean recommended,
        boolean issuesResolved,
        boolean wouldRentAgain
) {
}

record AddReplyCommand(
        @NotBlank String body,
        Long parentCommentId
) {
}

record VoteCommand(@NotNull ReviewVoteType type) {
}

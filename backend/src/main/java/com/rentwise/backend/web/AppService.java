package com.rentwise.backend.web;

import com.rentwise.backend.auth.RentwisePrincipal;
import com.rentwise.backend.location.*;
import com.rentwise.backend.landlord.Landlord;
import com.rentwise.backend.property.Property;
import com.rentwise.backend.property.PropertyStatus;
import com.rentwise.backend.property.PropertyRepository;
import com.rentwise.backend.review.Review;
import com.rentwise.backend.review.ReviewComment;
import com.rentwise.backend.review.ReviewCommentRepository;
import com.rentwise.backend.review.ReviewRepository;
import com.rentwise.backend.review.ReviewVote;
import com.rentwise.backend.review.ReviewVoteRepository;
import com.rentwise.backend.review.ReviewVoteType;
import com.rentwise.backend.user.AppUser;
import com.rentwise.backend.user.AppUserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppService {
    private final PropertyRepository propertyRepository;
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final CityRepository cityRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewCommentRepository reviewCommentRepository;
    private final ReviewVoteRepository reviewVoteRepository;
    private final AppUserRepository appUserRepository;

    public AppService(
            PropertyRepository propertyRepository,
            CountryRepository countryRepository,
            StateRepository stateRepository,
            CityRepository cityRepository,
            ReviewRepository reviewRepository,
            ReviewCommentRepository reviewCommentRepository,
            ReviewVoteRepository reviewVoteRepository,
            AppUserRepository appUserRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.cityRepository = cityRepository;
        this.reviewRepository = reviewRepository;
        this.reviewCommentRepository = reviewCommentRepository;
        this.reviewVoteRepository = reviewVoteRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<String> countries() {
        return countryRepository.findAllByOrderByName().stream()
                .map(Country::getName)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> states(String country) {
        if (!hasText(country)) {
            return List.of();
        }
        return countryRepository.findByNameIgnoreCase(country.trim())
                .map(found -> stateRepository.findByCountryIdOrderByName(found.getId()).stream()
                        .map(State::getName)
                        .toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<String> cities(String country, String state) {
        if (!hasText(country) || !hasText(state)) {
            return List.of();
        }
        return countryRepository.findByNameIgnoreCase(country.trim())
                .flatMap(foundCountry -> stateRepository.findByCountryIdAndName(foundCountry.getId(), state.trim()))
                .map(foundState -> cityRepository.findByStateIdOrderByName(foundState.getId()).stream()
                        .map(City::getName)
                        .toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<String> localities(String state, String city) {
        return propertyRepository.findDistinctLocalities(state, city, PropertyStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<PropertyCardDto> search(String state, String city, String locality) {
        return propertyRepository.search(
                        normalize(state),
                        normalize(city),
                        normalize(locality),
                        PropertyStatus.ACTIVE
                ).stream()
                .map(property -> {
                    List<Review> reviews = reviewRepository.findByPropertyIdOrderByCreatedAtDesc(property.getId());
                    Map<Long, List<ReviewVote>> votesByReviewId = new HashMap<>();
                    if (!reviews.isEmpty()) {
                        List<ReviewVote> votes = reviewVoteRepository.findByReviewIdIn(reviews.stream().map(Review::getId).toList());
                        for (ReviewVote vote : votes) {
                            votesByReviewId.computeIfAbsent(vote.getReview().getId(), ignored -> new ArrayList<>()).add(vote);
                        }
                    }
                    ScorecardDto scorecard = buildScorecard(reviews, votesByReviewId);
                    return new PropertyCardDto(
                            property.getId(),
                            property.getTitle(),
                            property.getPropertyType(),
                            property.getAddressLine1(),
                            property.getLocality(),
                            property.getCity(),
                            property.getState(),
                            property.getPostalCode(),
                            property.getHighlights(),
                            property.getLandlord().getName(),
                            property.getStatus(),
                            property.getOnboardingDate(),
                            scorecard
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PropertyDetailDto propertyDetail(Long propertyId, Authentication authentication) {
        Property property = propertyRepository.findDetailedById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        Long currentUserId = currentUserId(authentication);
        return buildPropertyDetail(property, currentUserId);
    }

    public ReviewDto createReview(Long propertyId, CreateReviewCommand command, Authentication authentication) {
        AppUser user = requireUser(authentication);
        Property property = propertyRepository.findDetailedById(propertyId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
        Review review = new Review(
                property,
                property.getLandlord(),
                user,
                command.headline().trim(),
                command.experienceSummary().trim(),
                command.problemsFaced().trim(),
                command.landlordSupport().trim(),
                command.leaseClosure().trim(),
                command.securityDepositOutcome().trim(),
                command.overallRating(),
                command.landlordRating(),
                command.propertyRating(),
                command.maintenanceRating(),
                command.moveOutRating(),
                command.depositRating(),
                command.recommended(),
                command.issuesResolved(),
                command.wouldRentAgain()
        );
        Review saved = reviewRepository.save(review);
        return mapReview(saved, List.of(), List.of(), user.getId());
    }

    public ReviewCommentDto addReply(Long reviewId, AddReplyCommand command, Authentication authentication) {
        AppUser user = requireUser(authentication);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        ReviewComment parent = null;
        if (command.parentCommentId() != null) {
            parent = reviewCommentRepository.findById(command.parentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            if (!Objects.equals(parent.getReview().getId(), reviewId)) {
                throw new IllegalArgumentException("Parent comment belongs to another review");
            }
        }
        ReviewComment comment = reviewCommentRepository.save(new ReviewComment(review, user, parent, command.body().trim()));
        return new ReviewCommentDto(comment.getId(), toSessionUser(user), comment.getBody(), comment.getCreatedAt(), List.of());
    }

    public VoteSummaryDto vote(Long reviewId, VoteCommand command, Authentication authentication) {
        AppUser user = requireUser(authentication);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));
        ReviewVote existing = reviewVoteRepository.findByReviewIdAndUserId(reviewId, user.getId()).orElse(null);
        if (existing != null && existing.getType() == command.type()) {
            reviewVoteRepository.delete(existing);
        } else if (existing != null) {
            existing.setType(command.type());
        } else {
            reviewVoteRepository.save(new ReviewVote(review, user, command.type()));
        }
        List<ReviewVote> votes = reviewVoteRepository.findByReviewIdIn(List.of(reviewId));
        return summarizeVotes(votes, user.getId());
    }

    private PropertyDetailDto buildPropertyDetail(Property property, Long currentUserId) {
        List<Review> reviews = reviewRepository.findByPropertyIdOrderByCreatedAtDesc(property.getId());
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        List<ReviewComment> comments = reviewIds.isEmpty() ? List.of() : reviewCommentRepository.findByReviewIdInOrderByCreatedAtAsc(reviewIds);
        List<ReviewVote> votes = reviewIds.isEmpty() ? List.of() : reviewVoteRepository.findByReviewIdIn(reviewIds);

        Map<Long, List<ReviewComment>> commentsByReviewId = new HashMap<>();
        for (ReviewComment comment : comments) {
            commentsByReviewId.computeIfAbsent(comment.getReview().getId(), ignored -> new ArrayList<>()).add(comment);
        }

        Map<Long, List<ReviewVote>> votesByReviewId = new HashMap<>();
        for (ReviewVote vote : votes) {
            votesByReviewId.computeIfAbsent(vote.getReview().getId(), ignored -> new ArrayList<>()).add(vote);
        }

        List<ReviewDto> reviewDtos = reviews.stream()
                .map(review -> mapReview(
                        review,
                        commentsByReviewId.getOrDefault(review.getId(), List.of()),
                        votesByReviewId.getOrDefault(review.getId(), List.of()),
                        currentUserId
                ))
                .toList();

        return new PropertyDetailDto(
                property.getId(),
                property.getTitle(),
                property.getPropertyType(),
                property.getAddressLine1(),
                property.getLocality(),
                property.getCity(),
                property.getState(),
                property.getPostalCode(),
                property.getHighlights(),
                property.getOnboardingDate(),
                property.getExitDate(),
                property.getMonthlyRent(),
                property.getDepositAmount(),
                property.getPropertyConditionOnEntry(),
                property.getPropertyConditionOnExit(),
                property.getAmenities(),
                property.getFurnishingType(),
                property.getOccupancyType(),
                property.getStatus(),
                property.getCreatedAt(),
                property.getUpdatedAt(),
                mapLandlord(property.getLandlord()),
                buildScorecard(reviews, votesByReviewId),
                reviewDtos,
                property.getCreatedBy() != null ? toSessionUser(property.getCreatedBy()) : null,
                property.getVerifiedBy() != null ? toSessionUser(property.getVerifiedBy()) : null,
                property.getVerifiedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private ReviewDto mapReview(Review review, List<ReviewComment> comments, List<ReviewVote> votes, Long currentUserId) {
        return new ReviewDto(
                review.getId(),
                toSessionUser(review.getAuthor()),
                review.getHeadline(),
                review.getExperienceSummary(),
                review.getProblemsFaced(),
                review.getLandlordSupport(),
                review.getLeaseClosure(),
                review.getSecurityDepositOutcome(),
                review.getOverallRating(),
                review.getLandlordRating(),
                review.getPropertyRating(),
                review.getMaintenanceRating(),
                review.getMoveOutRating(),
                review.getDepositRating(),
                review.isRecommended(),
                review.isIssuesResolved(),
                review.isWouldRentAgain(),
                review.getCreatedAt(),
                summarizeVotes(votes, currentUserId),
                buildCommentThread(comments)
        );
    }

    private List<ReviewCommentDto> buildCommentThread(List<ReviewComment> comments) {
        Map<Long, List<ReviewComment>> byParent = new HashMap<>();
        for (ReviewComment comment : comments) {
            Long parentId = comment.getParent() == null ? 0L : comment.getParent().getId();
            byParent.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(comment);
        }
        return byParent.getOrDefault(0L, List.of()).stream()
                .sorted(Comparator.comparing(ReviewComment::getCreatedAt))
                .map(comment -> mapComment(comment, byParent))
                .toList();
    }

    private ReviewCommentDto mapComment(ReviewComment comment, Map<Long, List<ReviewComment>> byParent) {
        List<ReviewCommentDto> replies = byParent.getOrDefault(comment.getId(), List.of()).stream()
                .sorted(Comparator.comparing(ReviewComment::getCreatedAt))
                .map(reply -> mapComment(reply, byParent))
                .toList();
        return new ReviewCommentDto(comment.getId(), toSessionUser(comment.getAuthor()), comment.getBody(), comment.getCreatedAt(), replies);
    }

    private VoteSummaryDto summarizeVotes(List<ReviewVote> votes, Long currentUserId) {
        long helpful = votes.stream().filter(vote -> vote.getType() == ReviewVoteType.HELPFUL).count();
        long notHelpful = votes.stream().filter(vote -> vote.getType() == ReviewVoteType.NOT_HELPFUL).count();
        long sameIssue = votes.stream().filter(vote -> vote.getType() == ReviewVoteType.SAME_ISSUE).count();
        ReviewVoteType currentVote = currentUserId == null ? null : votes.stream()
                .filter(vote -> Objects.equals(vote.getUser().getId(), currentUserId))
                .map(ReviewVote::getType)
                .findFirst()
                .orElse(null);
        return new VoteSummaryDto(helpful, notHelpful, sameIssue, currentVote);
    }

    private ScorecardDto buildScorecard(List<Review> reviews, Map<Long, List<ReviewVote>> votesByReviewId) {
        if (reviews.isEmpty()) {
            return new ScorecardDto(0, 0, 0, "Not enough reviews yet", 0, 0, 0);
        }
        int reviewCount = reviews.size();
        int unresolvedIssues = (int) reviews.stream().filter(review -> !review.isIssuesResolved()).count();
        int depositDisputes = (int) reviews.stream().filter(review -> review.getDepositRating() <= 2).count();
        int overall = weightedScore(reviews, votesByReviewId, Review::getOverallRating);
        int landlord = weightedScore(
                reviews,
                votesByReviewId,
                review -> (review.getLandlordRating() + review.getMaintenanceRating() + review.getMoveOutRating() + review.getDepositRating()) / 4.0
        );
        int property = weightedScore(reviews, votesByReviewId, Review::getPropertyRating);
        String recommendation;
        if (overall >= 82 && unresolvedIssues <= 1 && depositDisputes == 0) {
            recommendation = "Recommended";
        } else if (overall >= 65) {
            recommendation = "Mixed; inspect reviews closely";
        } else {
            recommendation = "High risk for tenancy";
        }
        return new ScorecardDto(overall, landlord, property, recommendation, reviewCount, unresolvedIssues, depositDisputes);
    }

    private int weightedScore(
            List<Review> reviews,
            Map<Long, List<ReviewVote>> votesByReviewId,
            ToDoubleFunction<Review> scoreExtractor
    ) {
        double weightedSum = 0;
        double totalWeight = 0;
        for (Review review : reviews) {
            List<ReviewVote> votes = votesByReviewId.getOrDefault(review.getId(), List.of());
            double weight = 1.0
                    + votes.stream().filter(v -> v.getType() == ReviewVoteType.HELPFUL).count() * 0.35
                    + votes.stream().filter(v -> v.getType() == ReviewVoteType.SAME_ISSUE).count() * 0.20
                    - votes.stream().filter(v -> v.getType() == ReviewVoteType.NOT_HELPFUL).count() * 0.15;
            weight = Math.max(0.5, weight);
            weightedSum += scoreExtractor.applyAsDouble(review) * weight;
            totalWeight += weight;
        }
        return (int) Math.round((weightedSum / totalWeight) * 20);
    }

    private AppUser requireUser(Authentication authentication) {
        Long userId = currentUserId(authentication);
        if (userId == null) {
            throw new IllegalArgumentException("Authentication required");
        }
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RentwisePrincipal principal)) {
            return null;
        }
        return principal.userId();
    }

    private SessionUserDto toSessionUser(AppUser user) {
        return new SessionUserDto(user.getId(), user.getDisplayName(), user.getEmail(), user.getMobileNumber(), user.getAvatarUrl(), user.isAdmin());
    }

    private LandlordDto mapLandlord(Landlord landlord) {
        return new LandlordDto(
                landlord.getId(),
                landlord.getName(),
                landlord.getEmail(),
                landlord.getPhoneNumber(),
                landlord.getManagementStyle()
        );
    }

    private String normalize(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}

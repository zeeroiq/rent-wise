package com.rentwise.backend.property;

import com.rentwise.backend.landlord.Landlord;
import com.rentwise.backend.landlord.LandlordRepository;
import com.rentwise.backend.location.CityRepository;
import com.rentwise.backend.location.State;
import com.rentwise.backend.location.StateRepository;
import com.rentwise.backend.review.Review;
import com.rentwise.backend.review.ReviewRepository;
import com.rentwise.backend.user.AppUser;
import com.rentwise.backend.web.PropertyDetailDto;
import com.rentwise.backend.web.PropertyCardDto;
import com.rentwise.backend.web.PropertyOnboardingCommand;
import com.rentwise.backend.web.LandlordDto;
import com.rentwise.backend.web.ScorecardDto;
import com.rentwise.backend.web.SessionUserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final LandlordRepository landlordRepository;
    private final StateRepository stateRepository;
    private final CityRepository cityRepository;
    private final ReviewRepository reviewRepository;

    public PropertyService(
            PropertyRepository propertyRepository,
            LandlordRepository landlordRepository,
            StateRepository stateRepository,
            CityRepository cityRepository,
            ReviewRepository reviewRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.landlordRepository = landlordRepository;
        this.stateRepository = stateRepository;
        this.cityRepository = cityRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Create a new property from tenant submission
     */
    public PropertyDetailDto createProperty(PropertyOnboardingCommand command, AppUser creator) {
        Landlord landlord = resolveLandlord(command);
        validateLocation(command.state(), command.city());

        Property property = new Property(
                command.title(),
                command.propertyType(),
                command.addressLine1(),
                command.locality(),
                command.city(),
                command.state(),
                command.postalCode(),
                command.highlights(),
                landlord,
                command.onboardingDate(),
                creator
        );

        property.setExitDate(command.exitDate());
        property.setMonthlyRent(command.monthlyRent());
        property.setDepositAmount(command.depositAmount());
        property.setPropertyConditionOnEntry(command.propertyConditionOnEntry());
        property.setPropertyConditionOnExit(command.propertyConditionOnExit());
        property.setAmenities(command.amenities());
        property.setFurnishingType(command.furnishingType());
        property.setOccupancyType(command.occupancyType());

        Property saved = propertyRepository.save(property);
        return toDetailDto(saved);
    }

    /**
     * Get property by ID
     */
    public PropertyDetailDto getPropertyById(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Property not found: " + id));
        return toDetailDto(property);
    }

    /**
     * Get all active properties
     */
    public Page<PropertyCardDto> getActiveProperties(Pageable pageable) {
        return propertyRepository.findByStatus(PropertyStatus.ACTIVE, pageable)
                .map(this::toCardDto);
    }

    /**
     * Get all pending properties for verification
     */
    public Page<PropertyCardDto> getPendingProperties(Pageable pageable) {
        return propertyRepository.findByStatus(PropertyStatus.PENDING_VERIFICATION, pageable)
                .map(this::toCardDto);
    }

    /**
     * Get all properties (admin view)
     */
    public Page<PropertyCardDto> getAllProperties(Pageable pageable) {
        return propertyRepository.findAll(pageable)
                .map(this::toCardDto);
    }

    /**
     * Get properties by status
     */
    public Page<PropertyCardDto> getPropertiesByStatus(PropertyStatus status, Pageable pageable) {
        return propertyRepository.findByStatus(status, pageable)
                .map(this::toCardDto);
    }

    /**
     * Get properties created by a specific user
     */
    public Page<PropertyCardDto> getPropertiesByCreator(AppUser creator, Pageable pageable) {
        return propertyRepository.findByCreatedBy(creator, pageable)
                .map(this::toCardDto);
    }

    /**
     * Get properties for a specific landlord
     */
    public Page<PropertyCardDto> getPropertiesByLandlord(Landlord landlord, Pageable pageable) {
        return propertyRepository.findByLandlord(landlord, pageable)
                .map(this::toCardDto);
    }

    /**
     * Verify a pending property
     */
    public PropertyDetailDto verifyProperty(Long propertyId, AppUser verifier) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NoSuchElementException("Property not found: " + propertyId));

        if (property.getStatus() != PropertyStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Property is not pending verification: " + propertyId);
        }

        property.setStatus(PropertyStatus.ACTIVE);
        property.setVerifiedBy(verifier);
        property.setVerifiedAt(LocalDateTime.now());
        property.setUpdatedAt(LocalDateTime.now());

        Property saved = propertyRepository.save(property);
        return toDetailDto(saved);
    }

    /**
     * Archive a property
     */
    public PropertyDetailDto archiveProperty(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NoSuchElementException("Property not found: " + propertyId));

        property.setStatus(PropertyStatus.ARCHIVED);
        property.setUpdatedAt(LocalDateTime.now());

        Property saved = propertyRepository.save(property);
        return toDetailDto(saved);
    }

    /**
     * Update property details
     */
    public PropertyDetailDto updateProperty(Long propertyId, PropertyOnboardingCommand command) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NoSuchElementException("Property not found: " + propertyId));
        validateLocation(command.state(), command.city());

        property.setTitle(command.title());
        property.setPropertyType(command.propertyType());
        property.setAddressLine1(command.addressLine1());
        property.setLocality(command.locality());
        property.setCity(command.city());
        property.setState(command.state());
        property.setPostalCode(command.postalCode());
        property.setHighlights(command.highlights());
        property.setOnboardingDate(command.onboardingDate());
        property.setExitDate(command.exitDate());
        property.setMonthlyRent(command.monthlyRent());
        property.setDepositAmount(command.depositAmount());
        property.setPropertyConditionOnEntry(command.propertyConditionOnEntry());
        property.setPropertyConditionOnExit(command.propertyConditionOnExit());
        property.setAmenities(command.amenities());
        property.setFurnishingType(command.furnishingType());
        property.setOccupancyType(command.occupancyType());
        property.setUpdatedAt(LocalDateTime.now());

        Property saved = propertyRepository.save(property);
        return toDetailDto(saved);
    }

    private Landlord resolveLandlord(PropertyOnboardingCommand command) {
        if (hasText(command.landlordName())) {
            return landlordRepository.save(new Landlord(
                    command.landlordName().trim(),
                    trimToNull(command.landlordEmail()),
                    trimToNull(command.landlordPhoneNumber()),
                    trimToNull(command.landlordManagementStyle())
            ));
        }
        if (command.landlordId() != null) {
            return landlordRepository.findById(command.landlordId())
                    .orElseThrow(() -> new NoSuchElementException("Landlord not found: " + command.landlordId()));
        }
        throw new IllegalArgumentException("Landlord details are required");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return Objects.equals(trimmed, "") ? null : trimmed;
    }

    private void validateLocation(String stateName, String cityName) {
        State state = stateRepository.findByNameIgnoreCase(stateName.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid state: " + stateName));
        cityRepository.findByStateIdAndNameIgnoreCase(state.getId(), cityName.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid city '" + cityName + "' for state '" + state.getName() + "'"
                ));
    }

    /**
     * Delete a property
     */
    public void deleteProperty(Long propertyId) {
        propertyRepository.deleteById(propertyId);
    }

    /**
     * Search properties
     */
    public List<PropertyCardDto> searchProperties(String query) {
        return propertyRepository.findByCityContainingIgnoreCaseOrLocalityContainingIgnoreCaseOrTitleContainingIgnoreCase(query, query, query)
                .stream()
                .map(this::toCardDto)
                .collect(Collectors.toList());
    }

    // Converter methods
    private PropertyDetailDto toDetailDto(Property property) {
        LandlordDto landlordDto = new LandlordDto(
                property.getLandlord().getId(),
                property.getLandlord().getName(),
                property.getLandlord().getEmail(),
                property.getLandlord().getPhoneNumber(),
                property.getLandlord().getManagementStyle()
        );

        SessionUserDto creatorDto = new SessionUserDto(
                property.getCreatedBy().getId(),
                property.getCreatedBy().getDisplayName(),
                property.getCreatedBy().getEmail(),
                property.getCreatedBy().getMobileNumber(),
                property.getCreatedBy().getAvatarUrl(),
                property.getCreatedBy().isAdmin()
        );

        SessionUserDto verifierDto = property.getVerifiedBy() != null ?
                new SessionUserDto(
                        property.getVerifiedBy().getId(),
                        property.getVerifiedBy().getDisplayName(),
                        property.getVerifiedBy().getEmail(),
                        property.getVerifiedBy().getMobileNumber(),
                        property.getVerifiedBy().getAvatarUrl(),
                        property.getVerifiedBy().isAdmin()
                ) : null;

        // Calculate scorecard (placeholder - would need review data)
        ScorecardDto scorecard = new ScorecardDto(0, 0, 0, "", 0, 0, 0);

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
                landlordDto,
                scorecard,
                List.of(),
                creatorDto,
                verifierDto,
                property.getVerifiedAt()
        );
    }

    private PropertyCardDto toCardDto(Property property) {
        ScorecardDto scorecard = calculateScorecard(property.getId());
        
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
    }
    
    private ScorecardDto calculateScorecard(Long propertyId) {
        List<Review> reviews = reviewRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId);
        
        if (reviews.isEmpty()) {
            return new ScorecardDto(0, 0, 0, "No reviews yet", 0, 0, 0);
        }
        
        int overallScore = (int) reviews.stream().mapToInt(Review::getOverallRating).average().orElse(0);
        int landlordScore = (int) reviews.stream().mapToInt(Review::getLandlordRating).average().orElse(0);
        int propertyScore = (int) reviews.stream().mapToInt(Review::getPropertyRating).average().orElse(0);
        
        long recommendedCount = reviews.stream().filter(Review::isRecommended).count();
        String recommendation = recommendedCount > reviews.size() / 2 ? "Recommended" : "Mixed Reviews";
        
        int unresolvedIssueCount = (int) reviews.stream().filter(r -> !r.isIssuesResolved()).count();
        int depositDisputeCount = (int) reviews.stream().filter(r -> r.getDepositRating() < 3).count();
        
        return new ScorecardDto(
                overallScore,
                landlordScore,
                propertyScore,
                recommendation,
                reviews.size(),
                unresolvedIssueCount,
                depositDisputeCount
        );
    }
}

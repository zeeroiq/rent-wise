package com.rentwise.backend.property;

import com.rentwise.backend.landlord.Landlord;
import com.rentwise.backend.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String propertyType;

    @Column(nullable = false)
    private String addressLine1;

    @Column(nullable = false)
    private String locality;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    private String postalCode;

    @Column(length = 1_500)
    private String highlights;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id")
    private Landlord landlord;

    // New tenant lifecycle fields
    @Column(nullable = false)
    private LocalDate onboardingDate;

    private LocalDate exitDate;

    private BigDecimal monthlyRent;

    private BigDecimal depositAmount;

    @Enumerated(EnumType.STRING)
    private PropertyCondition propertyConditionOnEntry;

    @Enumerated(EnumType.STRING)
    private PropertyCondition propertyConditionOnExit;

    @Column(columnDefinition = "TEXT")
    private String amenities;

    @Enumerated(EnumType.STRING)
    private FurnishingType furnishingType;

    @Enumerated(EnumType.STRING)
    private OccupancyType occupancyType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'ACTIVE'")
    private PropertyStatus status = PropertyStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_user_id")
    private AppUser verifiedBy;

    private LocalDateTime verifiedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected Property() {
    }

    public Property(
            String title,
            String propertyType,
            String addressLine1,
            String locality,
            String city,
            String state,
            String postalCode,
            String highlights,
            Landlord landlord,
            LocalDate onboardingDate,
            AppUser createdBy
    ) {
        this.title = title;
        this.propertyType = propertyType;
        this.addressLine1 = addressLine1;
        this.locality = locality;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.highlights = highlights;
        this.landlord = landlord;
        this.onboardingDate = onboardingDate;
        this.createdBy = createdBy;
        this.status = PropertyStatus.PENDING_VERIFICATION;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getLocality() {
        return locality;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getHighlights() {
        return highlights;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public void setHighlights(String highlights) {
        this.highlights = highlights;
    }

    public Landlord getLandlord() {
        return landlord;
    }

    public LocalDate getOnboardingDate() {
        return onboardingDate;
    }

    public void setOnboardingDate(LocalDate onboardingDate) {
        this.onboardingDate = onboardingDate;
    }

    public LocalDate getExitDate() {
        return exitDate;
    }

    public void setExitDate(LocalDate exitDate) {
        this.exitDate = exitDate;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public PropertyCondition getPropertyConditionOnEntry() {
        return propertyConditionOnEntry;
    }

    public void setPropertyConditionOnEntry(PropertyCondition propertyConditionOnEntry) {
        this.propertyConditionOnEntry = propertyConditionOnEntry;
    }

    public PropertyCondition getPropertyConditionOnExit() {
        return propertyConditionOnExit;
    }

    public void setPropertyConditionOnExit(PropertyCondition propertyConditionOnExit) {
        this.propertyConditionOnExit = propertyConditionOnExit;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public FurnishingType getFurnishingType() {
        return furnishingType;
    }

    public void setFurnishingType(FurnishingType furnishingType) {
        this.furnishingType = furnishingType;
    }

    public OccupancyType getOccupancyType() {
        return occupancyType;
    }

    public void setOccupancyType(OccupancyType occupancyType) {
        this.occupancyType = occupancyType;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
    }

    public PropertyStatus getStatus() {
        return status;
    }

    public void setStatus(PropertyStatus status) {
        this.status = status;
    }

    public AppUser getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(AppUser verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

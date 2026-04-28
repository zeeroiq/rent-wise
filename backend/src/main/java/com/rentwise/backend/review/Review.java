package com.rentwise.backend.review;

import com.rentwise.backend.landlord.Landlord;
import com.rentwise.backend.property.Property;
import com.rentwise.backend.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id")
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id")
    private Landlord landlord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(nullable = false)
    private String headline;

    @Column(nullable = false, length = 2_000)
    private String experienceSummary;

    @Column(nullable = false, length = 2_000)
    private String problemsFaced;

    @Column(nullable = false, length = 2_000)
    private String landlordSupport;

    @Column(nullable = false, length = 2_000)
    private String leaseClosure;

    @Column(nullable = false, length = 2_000)
    private String securityDepositOutcome;

    private int overallRating;
    private int landlordRating;
    private int propertyRating;
    private int maintenanceRating;
    private int moveOutRating;
    private int depositRating;
    private boolean recommended;
    private boolean issuesResolved;
    private boolean wouldRentAgain;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Review() {
    }

    public Review(
            Property property,
            Landlord landlord,
            AppUser author,
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
            boolean wouldRentAgain
    ) {
        this.property = property;
        this.landlord = landlord;
        this.author = author;
        this.headline = headline;
        this.experienceSummary = experienceSummary;
        this.problemsFaced = problemsFaced;
        this.landlordSupport = landlordSupport;
        this.leaseClosure = leaseClosure;
        this.securityDepositOutcome = securityDepositOutcome;
        this.overallRating = overallRating;
        this.landlordRating = landlordRating;
        this.propertyRating = propertyRating;
        this.maintenanceRating = maintenanceRating;
        this.moveOutRating = moveOutRating;
        this.depositRating = depositRating;
        this.recommended = recommended;
        this.issuesResolved = issuesResolved;
        this.wouldRentAgain = wouldRentAgain;
    }

    public Long getId() {
        return id;
    }

    public Property getProperty() {
        return property;
    }

    public Landlord getLandlord() {
        return landlord;
    }

    public AppUser getAuthor() {
        return author;
    }

    public String getHeadline() {
        return headline;
    }

    public String getExperienceSummary() {
        return experienceSummary;
    }

    public String getProblemsFaced() {
        return problemsFaced;
    }

    public String getLandlordSupport() {
        return landlordSupport;
    }

    public String getLeaseClosure() {
        return leaseClosure;
    }

    public String getSecurityDepositOutcome() {
        return securityDepositOutcome;
    }

    public int getOverallRating() {
        return overallRating;
    }

    public int getLandlordRating() {
        return landlordRating;
    }

    public int getPropertyRating() {
        return propertyRating;
    }

    public int getMaintenanceRating() {
        return maintenanceRating;
    }

    public int getMoveOutRating() {
        return moveOutRating;
    }

    public int getDepositRating() {
        return depositRating;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public boolean isIssuesResolved() {
        return issuesResolved;
    }

    public boolean isWouldRentAgain() {
        return wouldRentAgain;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

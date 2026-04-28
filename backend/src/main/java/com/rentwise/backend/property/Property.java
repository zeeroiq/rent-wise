package com.rentwise.backend.property;

import com.rentwise.backend.landlord.Landlord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

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
            Landlord landlord
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

    public Landlord getLandlord() {
        return landlord;
    }
}

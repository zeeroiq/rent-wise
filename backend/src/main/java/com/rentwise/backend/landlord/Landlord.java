package com.rentwise.backend.landlord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Landlord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String email;

    private String phoneNumber;

    @Column(length = 1_500)
    private String managementStyle;

    protected Landlord() {
    }

    public Landlord(String name, String email, String phoneNumber, String managementStyle) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.managementStyle = managementStyle;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getManagementStyle() {
        return managementStyle;
    }
}

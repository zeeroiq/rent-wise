package com.rentwise.backend.landlord;

import com.rentwise.backend.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

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

    @OneToOne
    @JoinColumn(name = "app_user_id", referencedColumnName = "id", unique = true, nullable = false)
    private AppUser appUser;

    protected Landlord() {
    }

    public Landlord(String name, String email, String phoneNumber, String managementStyle, AppUser appUser) {
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.managementStyle = managementStyle;
        this.appUser = appUser;
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

    public AppUser getAppUser() {
        return appUser;
    }
}

package com.rentwise.backend.web;

import com.rentwise.backend.auth.RentwisePrincipal;
import com.rentwise.backend.property.*;
import com.rentwise.backend.user.AppUser;
import com.rentwise.backend.user.AppUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/properties")
@CrossOrigin(origins = "*", maxAge = 3600)
public class PropertyController {

    private final PropertyService propertyService;
    private final AppUserRepository appUserRepository;

    public PropertyController(PropertyService propertyService, AppUserRepository appUserRepository) {
        this.propertyService = propertyService;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Create a new property (tenant-initiated)
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PropertyDetailDto> createProperty(
            @RequestBody PropertyOnboardingCommand command,
            Authentication auth
    ) {
        RentwisePrincipal principal = (RentwisePrincipal) auth.getPrincipal();
        AppUser user = appUserRepository.findById(principal.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        PropertyDetailDto created = propertyService.createProperty(command, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get property by ID (public)
     */
    @GetMapping("/{id}")
    public ResponseEntity<PropertyDetailDto> getProperty(@PathVariable Long id) {
        PropertyDetailDto property = propertyService.getPropertyById(id);
        return ResponseEntity.ok(property);
    }

    /**
     * Get all active properties (public)
     */
    @GetMapping
    public ResponseEntity<Page<PropertyCardDto>> getActiveProperties(Pageable pageable) {
        Page<PropertyCardDto> properties = propertyService.getActiveProperties(pageable);
        return ResponseEntity.ok(properties);
    }

    /**
     * Get all pending properties (admin/landlord only)
     */
    @GetMapping("/pending/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PropertyCardDto>> getPendingProperties(Pageable pageable) {
        Page<PropertyCardDto> properties = propertyService.getPendingProperties(pageable);
        return ResponseEntity.ok(properties);
    }

    /**
     * Get all properties by status (admin only)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PropertyCardDto>> getPropertiesByStatus(
            @PathVariable PropertyStatus status,
            Pageable pageable
    ) {
        Page<PropertyCardDto> properties = propertyService.getPropertiesByStatus(status, pageable);
        return ResponseEntity.ok(properties);
    }

    /**
     * Get all properties (admin only)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PropertyCardDto>> getAllProperties(Pageable pageable) {
        Page<PropertyCardDto> properties = propertyService.getAllProperties(pageable);
        return ResponseEntity.ok(properties);
    }

    /**
     * Get my properties (created by current user)
     */
    @GetMapping("/my/properties")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PropertyCardDto>> getMyProperties(
            Pageable pageable,
            Authentication auth
    ) {
        RentwisePrincipal principal = (RentwisePrincipal) auth.getPrincipal();
        AppUser user = appUserRepository.findById(principal.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        Page<PropertyCardDto> properties = propertyService.getPropertiesByCreator(user, pageable);
        return ResponseEntity.ok(properties);
    }

    /**
     * Search properties by query string
     */
    @GetMapping("/search")
    public ResponseEntity<List<PropertyCardDto>> searchProperties(@RequestParam String q) {
        List<PropertyCardDto> results = propertyService.searchProperties(q);
        return ResponseEntity.ok(results);
    }

    /**
     * Verify a pending property (admin only)
     */
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PropertyDetailDto> verifyProperty(
            @PathVariable Long id,
            Authentication auth
    ) {
        RentwisePrincipal principal = (RentwisePrincipal) auth.getPrincipal();
        AppUser user = appUserRepository.findById(principal.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        PropertyDetailDto verified = propertyService.verifyProperty(id, user);
        return ResponseEntity.ok(verified);
    }

    /**
     * Update property details
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PropertyDetailDto> updateProperty(
            @PathVariable Long id,
            @RequestBody PropertyOnboardingCommand command
    ) {
        PropertyDetailDto updated = propertyService.updateProperty(id, command);
        return ResponseEntity.ok(updated);
    }

    /**
     * Archive a property (admin only)
     */
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PropertyDetailDto> archiveProperty(@PathVariable Long id) {
        PropertyDetailDto archived = propertyService.archiveProperty(id);
        return ResponseEntity.ok(archived);
    }

    /**
     * Delete a property (admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id) {
        propertyService.deleteProperty(id);
        return ResponseEntity.noContent().build();
    }
}

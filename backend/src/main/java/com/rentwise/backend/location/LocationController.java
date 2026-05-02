package com.rentwise.backend.location;

import com.rentwise.backend.auth.RentwisePrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/locations")
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    // Countries
    @GetMapping("/countries")
    public List<CountryDto> getAllCountries() {
        return locationService.getAllCountries();
    }

    @GetMapping("/countries/{countryId}")
    public CountryDto getCountry(@PathVariable Long countryId) {
        return locationService.getCountryById(countryId);
    }

    @PostMapping("/countries")
    public CountryDto createCountry(
            @Valid @RequestBody CreateCountryCommand command,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return locationService.createCountry(command);
    }

    @DeleteMapping("/countries/{countryId}")
    public void deleteCountry(
            @PathVariable Long countryId,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        locationService.deleteCountry(countryId);
    }

    // States
    @GetMapping("/countries/{countryId}/states")
    public List<StateDto> getStatesByCountry(@PathVariable Long countryId) {
        return locationService.getStatesByCountry(countryId);
    }

    @GetMapping("/states/{stateId}")
    public StateDto getState(@PathVariable Long stateId) {
        return locationService.getStateById(stateId);
    }

    @PostMapping("/states")
    public StateDto createState(
            @Valid @RequestBody CreateStateCommand command,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return locationService.createState(command);
    }

    @DeleteMapping("/states/{stateId}")
    public void deleteState(
            @PathVariable Long stateId,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        locationService.deleteState(stateId);
    }

    // Cities
    @GetMapping("/states/{stateId}/cities")
    public List<CityDto> getCitiesByState(@PathVariable Long stateId) {
        return locationService.getCitiesByState(stateId);
    }

    @GetMapping("/cities/{cityId}")
    public CityDto getCity(@PathVariable Long cityId) {
        return locationService.getCityById(cityId);
    }

    @PostMapping("/cities")
    public CityDto createCity(
            @Valid @RequestBody CreateCityCommand command,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return locationService.createCity(command);
    }

    @DeleteMapping("/cities/{cityId}")
    public void deleteCity(
            @PathVariable Long cityId,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        locationService.deleteCity(cityId);
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof RentwisePrincipal principal)) {
            throw new IllegalAccessError("Authentication required");
        }
        if (!principal.isAdmin()) {
            throw new IllegalAccessError("Admin privileges required");
        }
    }
}


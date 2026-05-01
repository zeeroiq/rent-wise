package com.rentwise.backend.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/states")
    public List<String> getStates() {
        return locationService.getStates();
    }

    @GetMapping("/cities")
    public List<String> getCitiesByState(@RequestParam String state) {
        return locationService.getCitiesByState(state);
    }
}

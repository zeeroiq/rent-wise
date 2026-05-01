package com.rentwise.backend.web;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LocationService {

    private final Map<String, Set<String>> statesAndCities = new HashMap<>();

    @PostConstruct
    public void init() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ClassPathResource("data/states_cities.csv").getInputStream()))) {
            // Skip header row
            reader.readLine();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String state = parts[0].trim();
                    String city = parts[1].trim();
                    statesAndCities.computeIfAbsent(state, k -> new HashSet<>()).add(city);
                }
            }
        } catch (IOException e) {
            // Log the error or throw a custom exception
            System.err.println("Failed to load states_cities.csv: " + e.getMessage());
        }
    }

    public List<String> getStates() {
        return statesAndCities.keySet().stream().sorted().collect(Collectors.toList());
    }

    public List<String> getCitiesByState(String state) {
        return statesAndCities.getOrDefault(state, Collections.emptySet()).stream().sorted().collect(Collectors.toList());
    }

    public boolean isValidState(String state) {
        return statesAndCities.containsKey(state);
    }

    public boolean isValidCity(String state, String city) {
        return statesAndCities.containsKey(state) && statesAndCities.get(state).contains(city);
    }
}

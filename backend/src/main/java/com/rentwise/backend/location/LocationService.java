package com.rentwise.backend.location;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LocationService {
    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final CityRepository cityRepository;

    public LocationService(
            CountryRepository countryRepository,
            StateRepository stateRepository,
            CityRepository cityRepository
    ) {
        this.countryRepository = countryRepository;
        this.stateRepository = stateRepository;
        this.cityRepository = cityRepository;
    }

    // Country operations
    @Transactional(readOnly = true)
    public List<CountryDto> getAllCountries() {
        return countryRepository.findAllByOrderByName().stream()
                .map(this::mapCountry)
                .toList();
    }

    @Transactional(readOnly = true)
    public CountryDto getCountryById(Long countryId) {
        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> new IllegalArgumentException("Country not found with id: " + countryId));
        return mapCountry(country);
    }

    public CountryDto createCountry(CreateCountryCommand command) {
        if (countryRepository.findByCode(command.code().toUpperCase()).isPresent()) {
            throw new IllegalArgumentException("Country with code " + command.code() + " already exists");
        }
        Country country = new Country(command.code().toUpperCase(), command.name());
        Country saved = countryRepository.save(country);
        return mapCountry(saved);
    }

    public void deleteCountry(Long countryId) {
        Country country = countryRepository.findById(countryId)
                .orElseThrow(() -> new IllegalArgumentException("Country not found with id: " + countryId));
        // Check if country has associated states
        List<State> states = stateRepository.findByCountryIdOrderByName(countryId);
        if (!states.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete country that has associated states");
        }
        countryRepository.delete(country);
    }

    // State operations
    @Transactional(readOnly = true)
    public List<StateDto> getStatesByCountry(Long countryId) {
        countryRepository.findById(countryId)
                .orElseThrow(() -> new IllegalArgumentException("Country not found with id: " + countryId));
        return stateRepository.findByCountryIdOrderByName(countryId).stream()
                .map(this::mapState)
                .toList();
    }

    @Transactional(readOnly = true)
    public StateDto getStateById(Long stateId) {
        State state = stateRepository.findById(stateId)
                .orElseThrow(() -> new IllegalArgumentException("State not found with id: " + stateId));
        return mapState(state);
    }

    public StateDto createState(CreateStateCommand command) {
        Country country = countryRepository.findById(command.countryId())
                .orElseThrow(() -> new IllegalArgumentException("Country not found with id: " + command.countryId()));
        if (stateRepository.findByCountryIdAndCode(command.countryId(), command.code().toUpperCase()).isPresent()) {
            throw new IllegalArgumentException("State with code " + command.code() + " already exists in this country");
        }
        State state = new State(country, command.code().toUpperCase(), command.name());
        State saved = stateRepository.save(state);
        return mapState(saved);
    }

    public void deleteState(Long stateId) {
        State state = stateRepository.findById(stateId)
                .orElseThrow(() -> new IllegalArgumentException("State not found with id: " + stateId));
        // Check if state has associated cities
        List<City> cities = cityRepository.findByStateIdOrderByName(stateId);
        if (!cities.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete state that has associated cities");
        }
        stateRepository.delete(state);
    }

    // City operations
    @Transactional(readOnly = true)
    public List<CityDto> getCitiesByState(Long stateId) {
        stateRepository.findById(stateId)
                .orElseThrow(() -> new IllegalArgumentException("State not found with id: " + stateId));
        return cityRepository.findByStateIdOrderByName(stateId).stream()
                .map(this::mapCity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CityDto getCityById(Long cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("City not found with id: " + cityId));
        return mapCity(city);
    }

    public CityDto createCity(CreateCityCommand command) {
        State state = stateRepository.findById(command.stateId())
                .orElseThrow(() -> new IllegalArgumentException("State not found with id: " + command.stateId()));
        if (cityRepository.findByStateIdAndCode(command.stateId(), command.code().toUpperCase()).isPresent()) {
            throw new IllegalArgumentException("City with code " + command.code() + " already exists in this state");
        }
        City city = new City(state, command.code().toUpperCase(), command.name());
        City saved = cityRepository.save(city);
        return mapCity(saved);
    }

    public void deleteCity(Long cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new IllegalArgumentException("City not found with id: " + cityId));
        cityRepository.delete(city);
    }

    // Mapping helpers
    private CountryDto mapCountry(Country country) {
        return new CountryDto(
                country.getId(),
                country.getCode(),
                country.getName(),
                country.getCreatedAt()
        );
    }

    private StateDto mapState(State state) {
        return new StateDto(
                state.getId(),
                state.getCountry().getId(),
                state.getCode(),
                state.getName(),
                state.getCreatedAt()
        );
    }

    private CityDto mapCity(City city) {
        return new CityDto(
                city.getId(),
                city.getState().getId(),
                city.getCode(),
                city.getName(),
                city.getCreatedAt()
        );
    }
}


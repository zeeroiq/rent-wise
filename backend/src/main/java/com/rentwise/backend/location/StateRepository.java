package com.rentwise.backend.location;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateRepository extends JpaRepository<State, Long> {
    Optional<State> findByCountryIdAndCode(Long countryId, String code);

    Optional<State> findByCountryIdAndName(Long countryId, String name);

    List<State> findByCountryIdOrderByName(Long countryId);

    List<State> findAllByOrderByName();
}


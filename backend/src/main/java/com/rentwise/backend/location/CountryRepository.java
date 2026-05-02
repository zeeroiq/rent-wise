package com.rentwise.backend.location;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, Long> {
    Optional<Country> findByCode(String code);

    Optional<Country> findByName(String name);

    List<Country> findAllByOrderByName();
}


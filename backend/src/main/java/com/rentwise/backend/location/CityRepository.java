package com.rentwise.backend.location;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {
    Optional<City> findByStateIdAndCode(Long stateId, String code);

    Optional<City> findByStateIdAndName(Long stateId, String name);
    Optional<City> findByStateIdAndNameIgnoreCase(Long stateId, String name);

    List<City> findByStateIdOrderByName(Long stateId);

    List<City> findAllByOrderByName();
}

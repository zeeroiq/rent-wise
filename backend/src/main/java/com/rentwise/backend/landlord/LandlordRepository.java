package com.rentwise.backend.landlord;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LandlordRepository extends JpaRepository<Landlord, Long> {
    Optional<Landlord> findByAppUserId(Long appUserId);
}

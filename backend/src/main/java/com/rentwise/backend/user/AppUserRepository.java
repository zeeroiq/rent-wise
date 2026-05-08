package com.rentwise.backend.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    Optional<AppUser> findByMobileNumber(String mobileNumber);

    Optional<AppUser> findByTelegramIdIgnoreCase(String telegramId);

    Optional<AppUser> findBySignalNumber(String signalNumber);
}

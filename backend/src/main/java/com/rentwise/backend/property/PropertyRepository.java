package com.rentwise.backend.property;

import com.rentwise.backend.landlord.Landlord;
import com.rentwise.backend.user.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    @Query("""
            select distinct p.state
            from Property p
            order by p.state
            """)
    List<String> findDistinctStates();

    @Query("""
            select distinct p.city
            from Property p
            where lower(p.state) = lower(:state)
            order by p.city
            """)
    List<String> findDistinctCities(@Param("state") String state);

    @Query("""
            select distinct p.locality
            from Property p
            where lower(p.state) = lower(:state)
              and lower(p.city) = lower(:city)
            order by p.locality
            """)
    List<String> findDistinctLocalities(@Param("state") String state, @Param("city") String city);

    @Query("""
            select p
            from Property p
            join fetch p.landlord
            where (:state is null or lower(p.state) = lower(:state))
              and (:city is null or lower(p.city) = lower(:city))
              and (:locality is null or lower(p.locality) = lower(:locality))
            order by p.state, p.city, p.locality, p.title
            """)
    List<Property> search(
            @Param("state") String state,
            @Param("city") String city,
            @Param("locality") String locality
    );

    @Query("""
            select p
            from Property p
            join fetch p.landlord
            where p.id = :id
            """)
    Optional<Property> findDetailedById(@Param("id") Long id);

    // New query methods for property status and filtering
    Page<Property> findByStatus(PropertyStatus status, Pageable pageable);

    Page<Property> findByCreatedBy(AppUser createdBy, Pageable pageable);

    Page<Property> findByLandlord(Landlord landlord, Pageable pageable);

    List<Property> findByCityContainingIgnoreCaseOrLocalityContainingIgnoreCaseOrTitleContainingIgnoreCase(
            String city,
            String locality,
            String title
    );
}

package com.rentwise.backend.property;

import com.rentwise.backend.auth.AuthProvider;
import com.rentwise.backend.landlord.Landlord;
import com.rentwise.backend.landlord.LandlordRepository;
import com.rentwise.backend.user.AppUser;
import com.rentwise.backend.user.AppUserRepository;
import com.rentwise.backend.web.PropertyCardDto;
import com.rentwise.backend.web.PropertyDetailDto;
import com.rentwise.backend.web.PropertyOnboardingCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class PropertyServiceTest {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private LandlordRepository landlordRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    private Landlord testLandlord;
    private AppUser testCreator;
    private AppUser testVerifier;

    @BeforeEach
    void setUp() {
        testLandlord = landlordRepository.save(new Landlord(
                "Test Landlord",
                "landlord@test.com",
                "+1-555-1234",
                "Professional management"
        ));

        testCreator = appUserRepository.save(new AppUser(
                "Creator User",
                "creator@test.com",
                null,
                AuthProvider.OTP_EMAIL,
                null,
                false
        ));

        testVerifier = appUserRepository.save(new AppUser(
                "Verifier User",
                "verifier@test.com",
                null,
                AuthProvider.OTP_EMAIL,
                null,
                true
        ));
    }

    // ============= CREATE TESTS =============
    @Test
    void testCreateProperty_Success() {
        PropertyOnboardingCommand command = new PropertyOnboardingCommand(
                "Test Apartment",
                "Apartment",
                "123 Main St",
                "Downtown",
                "New York",
                "NY",
                "10001",
                "Spacious corner apartment",
                testLandlord.getId(),
                null,
                null,
                null,
                null,
                LocalDate.of(2023, 1, 15),
                LocalDate.of(2024, 1, 14),
                BigDecimal.valueOf(2500),
                BigDecimal.valueOf(5000),
                PropertyCondition.EXCELLENT,
                PropertyCondition.GOOD,
                "WiFi,Parking,Gym,Balcony",
                FurnishingType.SEMI_FURNISHED,
                OccupancyType.SOLO
        );

        PropertyDetailDto created = propertyService.createProperty(command, testCreator);

        assertNotNull(created.id());
        assertEquals("Test Apartment", created.title());
        assertEquals(PropertyStatus.PENDING_VERIFICATION, created.status());
        assertEquals(BigDecimal.valueOf(2500), created.monthlyRent());
        assertEquals(BigDecimal.valueOf(5000), created.depositAmount());
        assertEquals(PropertyCondition.EXCELLENT, created.propertyConditionOnEntry());
        assertEquals(FurnishingType.SEMI_FURNISHED, created.furnishingType());
        assertEquals(OccupancyType.SOLO, created.occupancyType());
    }

    @Test
    void testCreateProperty_InvalidLandlord() {
        PropertyOnboardingCommand command = new PropertyOnboardingCommand(
                "Test Apartment",
                "Apartment",
                "123 Main St",
                "Downtown",
                "New York",
                "NY",
                "10001",
                "Spacious corner apartment",
                999999L,
                null,
                null,
                null,
                null,
                LocalDate.of(2023, 1, 15),
                null,
                BigDecimal.valueOf(2500),
                BigDecimal.valueOf(5000),
                PropertyCondition.EXCELLENT,
                null,
                "WiFi,Parking",
                FurnishingType.SEMI_FURNISHED,
                OccupancyType.SOLO
        );

        assertThrows(NoSuchElementException.class, () -> propertyService.createProperty(command, testCreator));
    }

    // ============= READ TESTS =============
    @Test
    void testGetPropertyById_Success() {
        Property savedProperty = propertyRepository.save(new Property(
                "Retrieval Test",
                "Studio",
                "456 Oak Ave",
                "Midtown",
                "New York",
                "NY",
                "10010",
                "Small but cozy",
                testLandlord,
                LocalDate.of(2023, 6, 1),
                testCreator
        ));
        savedProperty.setMonthlyRent(BigDecimal.valueOf(1800));
        savedProperty.setStatus(PropertyStatus.ACTIVE);
        propertyRepository.save(savedProperty);

        PropertyDetailDto retrieved = propertyService.getPropertyById(savedProperty.getId());

        assertNotNull(retrieved);
        assertEquals("Retrieval Test", retrieved.title());
        assertEquals(BigDecimal.valueOf(1800), retrieved.monthlyRent());
    }

    @Test
    void testGetPropertyById_NotFound() {
        assertThrows(NoSuchElementException.class, () -> propertyService.getPropertyById(999999L));
    }

    @Test
    void testGetActiveProperties() {
        Property active1 = propertyRepository.save(new Property(
                "Active 1",
                "Apartment",
                "1 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60601",
                "First active",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));
        active1.setStatus(PropertyStatus.ACTIVE);
        propertyRepository.save(active1);

        Property archived = propertyRepository.save(new Property(
                "Archived",
                "Apartment",
                "2 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60602",
                "Archived one",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));
        archived.setStatus(PropertyStatus.ARCHIVED);
        propertyRepository.save(archived);

        Pageable pageable = PageRequest.of(0, 10);
        Page<PropertyCardDto> activeProperties = propertyService.getActiveProperties(pageable);

        assertTrue(activeProperties.getContent().size() >= 1);
        assertTrue(activeProperties.getContent().stream()
                .anyMatch(p -> p.title().equals("Active 1")));
    }

    @Test
    void testGetPropertiesByStatus() {
        Property pending = propertyRepository.save(new Property(
                "Pending Property",
                "Apartment",
                "3 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60603",
                "Pending verification",
                testLandlord,
                LocalDate.of(2024, 1, 1),
                testCreator
        ));
        pending.setStatus(PropertyStatus.PENDING_VERIFICATION);
        propertyRepository.save(pending);

        Pageable pageable = PageRequest.of(0, 10);
        Page<PropertyCardDto> pendingProperties = propertyService.getPropertiesByStatus(PropertyStatus.PENDING_VERIFICATION, pageable);

        assertTrue(pendingProperties.getContent().stream()
                .anyMatch(p -> p.title().equals("Pending Property")));
    }

    @Test
    void testGetPropertiesByCreator() {
        Property prop1 = propertyRepository.save(new Property(
                "Creator Test 1",
                "Apartment",
                "4 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60604",
                "First",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));

        Pageable pageable = PageRequest.of(0, 10);
        Page<PropertyCardDto> createdProperties = propertyService.getPropertiesByCreator(testCreator, pageable);

        assertTrue(createdProperties.getContent().stream()
                .anyMatch(p -> p.title().equals("Creator Test 1")));
    }

    @Test
    void testGetPropertiesByLandlord() {
        Property prop1 = propertyRepository.save(new Property(
                "Landlord Test",
                "Apartment",
                "5 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60605",
                "Landlord property",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));

        Pageable pageable = PageRequest.of(0, 10);
        Page<PropertyCardDto> landlordProperties = propertyService.getPropertiesByLandlord(testLandlord, pageable);

        assertTrue(landlordProperties.getContent().stream()
                .anyMatch(p -> p.title().equals("Landlord Test")));
    }

    // ============= UPDATE TESTS =============
    @Test
    void testUpdateProperty_Fields() {
        Property original = propertyRepository.save(new Property(
                "Original",
                "Apartment",
                "6 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60606",
                "Original",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));
        original.setMonthlyRent(BigDecimal.valueOf(2000));
        original.setStatus(PropertyStatus.ACTIVE);
        propertyRepository.save(original);

        PropertyOnboardingCommand updateCommand = new PropertyOnboardingCommand(
                "Updated",
                "Apartment",
                "6 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60606",
                "Updated",
                testLandlord.getId(),
                null,
                null,
                null,
                null,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2024, 12, 31),
                BigDecimal.valueOf(2500),
                BigDecimal.valueOf(5000),
                PropertyCondition.GOOD,
                PropertyCondition.FAIR,
                "WiFi,Parking",
                FurnishingType.SEMI_FURNISHED,
                OccupancyType.SOLO
        );

        PropertyDetailDto updated = propertyService.updateProperty(original.getId(), updateCommand);

        assertEquals("Updated", updated.title());
        assertEquals(BigDecimal.valueOf(2500), updated.monthlyRent());
        assertEquals(BigDecimal.valueOf(5000), updated.depositAmount());
        assertEquals(LocalDate.of(2024, 12, 31), updated.exitDate());
        assertEquals(PropertyCondition.FAIR, updated.propertyConditionOnExit());
    }

    @Test
    void testUpdateProperty_NotFound() {
        PropertyOnboardingCommand command = new PropertyOnboardingCommand(
                "Test",
                "Apartment",
                "123 Main",
                "Downtown",
                "New York",
                "NY",
                "10001",
                "Test",
                testLandlord.getId(),
                null,
                null,
                null,
                null,
                LocalDate.of(2023, 1, 1),
                null,
                BigDecimal.valueOf(2000),
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThrows(NoSuchElementException.class, () -> propertyService.updateProperty(999999L, command));
    }

    // ============= VERIFICATION TESTS =============
    @Test
    void testVerifyProperty_Success() {
        Property pending = propertyRepository.save(new Property(
                "Pending Verification",
                "Apartment",
                "7 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60607",
                "Needs verification",
                testLandlord,
                LocalDate.of(2024, 1, 1),
                testCreator
        ));
        pending.setStatus(PropertyStatus.PENDING_VERIFICATION);
        propertyRepository.save(pending);

        PropertyDetailDto verified = propertyService.verifyProperty(pending.getId(), testVerifier);

        assertEquals(PropertyStatus.ACTIVE, verified.status());
        assertNotNull(verified.verifiedAt());
        assertEquals(testVerifier.getId(), verified.verifiedBy().id());
    }

    @Test
    void testVerifyProperty_NotPending() {
        Property active = propertyRepository.save(new Property(
                "Already Active",
                "Apartment",
                "8 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60608",
                "Already active",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));
        active.setStatus(PropertyStatus.ACTIVE);
        propertyRepository.save(active);

        assertThrows(IllegalStateException.class, () -> propertyService.verifyProperty(active.getId(), testVerifier));
    }

    @Test
    void testVerifyProperty_NotFound() {
        assertThrows(NoSuchElementException.class, () -> propertyService.verifyProperty(999999L, testVerifier));
    }

    // ============= STATUS TRANSITION TESTS =============
    @Test
    void testArchiveProperty_Success() {
        Property active = propertyRepository.save(new Property(
                "To Archive",
                "Apartment",
                "9 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60609",
                "Will be archived",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));
        active.setStatus(PropertyStatus.ACTIVE);
        propertyRepository.save(active);

        PropertyDetailDto archived = propertyService.archiveProperty(active.getId());

        assertEquals(PropertyStatus.ARCHIVED, archived.status());
    }

    @Test
    void testArchiveProperty_NotFound() {
        assertThrows(NoSuchElementException.class, () -> propertyService.archiveProperty(999999L));
    }

    // ============= DELETE TESTS =============
    @Test
    void testDeleteProperty_Success() {
        Property toDelete = propertyRepository.save(new Property(
                "To Delete",
                "Apartment",
                "10 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60610",
                "To be deleted",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));

        propertyService.deleteProperty(toDelete.getId());

        assertTrue(propertyRepository.findById(toDelete.getId()).isEmpty());
    }

    // ============= SEARCH TESTS =============
    @Test
    void testSearchProperties_ByCity() {
        Property chicagoProperty = propertyRepository.save(new Property(
                "Chicago Search Test",
                "Apartment",
                "11 Oak St",
                "Downtown",
                "Chicago",
                "IL",
                "60611",
                "Search test",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));

        var results = propertyService.searchProperties("Chicago");

        assertTrue(results.stream()
                .anyMatch(p -> p.title().equals("Chicago Search Test")));
    }

    @Test
    void testSearchProperties_ByLocality() {
        Property localityProperty = propertyRepository.save(new Property(
                "Locality Search",
                "Apartment",
                "12 Oak St",
                "MidCity",
                "New York",
                "NY",
                "10001",
                "Search test",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));

        var results = propertyService.searchProperties("MidCity");

        assertTrue(results.stream()
                .anyMatch(p -> p.title().equals("Locality Search")));
    }

    @Test
    void testSearchProperties_ByTitle() {
        Property titleProperty = propertyRepository.save(new Property(
                "Unique Title Property",
                "Apartment",
                "13 Oak St",
                "Downtown",
                "Boston",
                "MA",
                "02101",
                "Search test",
                testLandlord,
                LocalDate.of(2023, 1, 1),
                testCreator
        ));

        var results = propertyService.searchProperties("Unique Title");

        assertTrue(results.stream()
                .anyMatch(p -> p.title().equals("Unique Title Property")));
    }

    // ============= INTEGRATION TESTS =============
    @Test
    void testPropertyLifecycle_FromCreationToArchive() {
        PropertyOnboardingCommand createCommand = new PropertyOnboardingCommand(
                "Lifecycle Property",
                "Apartment",
                "14 Oak St",
                "Downtown",
                "San Francisco",
                "CA",
                "94102",
                "Testing full lifecycle",
                testLandlord.getId(),
                null,
                null,
                null,
                null,
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2024, 12, 31),
                BigDecimal.valueOf(3500),
                BigDecimal.valueOf(7000),
                PropertyCondition.EXCELLENT,
                PropertyCondition.GOOD,
                "WiFi,Parking,Gym",
                FurnishingType.FULLY_FURNISHED,
                OccupancyType.FAMILY
        );

        PropertyDetailDto created = propertyService.createProperty(createCommand, testCreator);
        assertEquals(PropertyStatus.PENDING_VERIFICATION, created.status());

        PropertyDetailDto verified = propertyService.verifyProperty(created.id(), testVerifier);
        assertEquals(PropertyStatus.ACTIVE, verified.status());
        assertNotNull(verified.verifiedBy());

        PropertyDetailDto archived = propertyService.archiveProperty(created.id());
        assertEquals(PropertyStatus.ARCHIVED, archived.status());
    }

    @Test
    void testPropertyLifecycle_WithExitData() {
        Property property = propertyRepository.save(new Property(
                "Exit Data Property",
                "Apartment",
                "15 Oak St",
                "Downtown",
                "Seattle",
                "WA",
                "98101",
                "With exit data",
                testLandlord,
                LocalDate.of(2023, 6, 1),
                testCreator
        ));
        property.setMonthlyRent(BigDecimal.valueOf(2800));
        property.setDepositAmount(BigDecimal.valueOf(5600));
        property.setPropertyConditionOnEntry(PropertyCondition.EXCELLENT);
        property.setPropertyConditionOnExit(PropertyCondition.FAIR);
        property.setExitDate(LocalDate.of(2024, 5, 31));
        property.setFurnishingType(FurnishingType.SEMI_FURNISHED);
        property.setOccupancyType(OccupancyType.SOLO);
        property.setAmenities("WiFi,Parking");
        property.setStatus(PropertyStatus.ARCHIVED);
        propertyRepository.save(property);

        PropertyDetailDto retrieved = propertyService.getPropertyById(property.getId());

        assertEquals(PropertyStatus.ARCHIVED, retrieved.status());
        assertEquals(LocalDate.of(2024, 5, 31), retrieved.exitDate());
        assertEquals(PropertyCondition.FAIR, retrieved.propertyConditionOnExit());
    }
}

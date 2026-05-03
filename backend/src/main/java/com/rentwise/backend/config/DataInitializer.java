package com.rentwise.backend.config;

import com.rentwise.backend.auth.AuthProvider;
import com.rentwise.backend.landlord.Landlord;
import com.rentwise.backend.landlord.LandlordRepository;
import com.rentwise.backend.location.City;
import com.rentwise.backend.location.CityRepository;
import com.rentwise.backend.location.Country;
import com.rentwise.backend.location.CountryRepository;
import com.rentwise.backend.location.State;
import com.rentwise.backend.location.StateRepository;
import com.rentwise.backend.property.Property;
import com.rentwise.backend.property.PropertyRepository;
import com.rentwise.backend.property.PropertyCondition;
import com.rentwise.backend.property.FurnishingType;
import com.rentwise.backend.property.OccupancyType;
import com.rentwise.backend.property.PropertyStatus;
import com.rentwise.backend.review.Review;
import com.rentwise.backend.review.ReviewComment;
import com.rentwise.backend.review.ReviewCommentRepository;
import com.rentwise.backend.review.ReviewRepository;
import com.rentwise.backend.review.ReviewVote;
import com.rentwise.backend.review.ReviewVoteRepository;
import com.rentwise.backend.review.ReviewVoteType;
import com.rentwise.backend.user.AppUser;
import com.rentwise.backend.user.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.LocalDate;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(
            AppUserRepository userRepository,
            LandlordRepository landlordRepository,
            PropertyRepository propertyRepository,
            ReviewRepository reviewRepository,
            ReviewCommentRepository reviewCommentRepository,
            ReviewVoteRepository reviewVoteRepository,
            CountryRepository countryRepository,
            StateRepository stateRepository,
            CityRepository cityRepository
    ) {
        return args -> {
            if (propertyRepository.count() > 0) {
                return;
            }

            // Create admin user
            userRepository.save(new AppUser("Admin", "admin@example.com", null, AuthProvider.OTP_EMAIL, null, true));
            AppUser aisha = userRepository.save(new AppUser("Aisha", "aisha@example.com", null, AuthProvider.OTP_EMAIL, null));
            AppUser karan = userRepository.save(new AppUser("Karan", "karan@example.com", null, AuthProvider.GOOGLE, null));
            AppUser meera = userRepository.save(new AppUser("Meera", null, "+15550001001", AuthProvider.OTP_MOBILE, null));

            // Create countries
            Country usa = countryRepository.save(new Country("US", "United States"));
            Country india = countryRepository.save(new Country("IN", "India"));

            // Create states for USA
            State california = stateRepository.save(new State(usa, "CA", "California"));
            State newyork = stateRepository.save(new State(usa, "NY", "New York"));
            State texas = stateRepository.save(new State(usa, "TX", "Texas"));
            State florida = stateRepository.save(new State(usa, "FL", "Florida"));
            State illinois = stateRepository.save(new State(usa, "IL", "Illinois"));

            // Create states for India
            State karnataka = stateRepository.save(new State(india, "KA", "Karnataka"));
            State maharashtra = stateRepository.save(new State(india, "MH", "Maharashtra"));

            // Create cities for USA states
            cityRepository.save(new City(california, "LA", "Los Angeles"));
            cityRepository.save(new City(california, "SF", "San Francisco"));
            cityRepository.save(new City(newyork, "NYC", "New York City"));
            cityRepository.save(new City(newyork, "BUF", "Buffalo"));
            cityRepository.save(new City(texas, "HOU", "Houston"));
            cityRepository.save(new City(texas, "DAL", "Dallas"));
            cityRepository.save(new City(florida, "MIA", "Miami"));
            cityRepository.save(new City(florida, "ORL", "Orlando"));
            cityRepository.save(new City(illinois, "CHI", "Chicago"));
            cityRepository.save(new City(illinois, "SPR", "Springfield"));

            // Create cities for India states
            cityRepository.save(new City(karnataka, "BLR", "Bengaluru"));
            cityRepository.save(new City(maharashtra, "MUM", "Mumbai"));

            Landlord patel = landlordRepository.save(new Landlord(
                    "Harish Patel",
                    "patel.rentals@example.com",
                    "+1-555-0100",
                    "Fast to lease, slower when maintenance affects only one tenant."
            ));
            Landlord fernandez = landlordRepository.save(new Landlord(
                    "Rosa Fernandez",
                    "rosa@example.com",
                    "+1-555-0122",
                    "Professional documentation and clear move-out checklist."
            ));

            Property indiranagar = propertyRepository.save(new Property(
                    "Cedar Point Residency",
                    "Apartment",
                    "14A 11th Main Road",
                    "Indiranagar",
                    "Bengaluru",
                    "Karnataka",
                    "560038",
                    "Walkable block, backup power, but narrow visitor parking.",
                    patel,
                    LocalDate.of(2023, 6, 15),
                    aisha
            ));
            indiranagar.setMonthlyRent(java.math.BigDecimal.valueOf(18000));
            indiranagar.setDepositAmount(java.math.BigDecimal.valueOf(36000));
            indiranagar.setPropertyConditionOnEntry(PropertyCondition.GOOD);
            indiranagar.setPropertyConditionOnExit(PropertyCondition.FAIR);
            indiranagar.setExitDate(LocalDate.of(2024, 12, 31));
            indiranagar.setFurnishingType(FurnishingType.SEMI_FURNISHED);
            indiranagar.setOccupancyType(OccupancyType.SOLO);
            indiranagar.setAmenities("WiFi,Parking,Gym,Balcony,AC");
            indiranagar.setStatus(PropertyStatus.ACTIVE);
            propertyRepository.save(indiranagar);

            Property koramangala = propertyRepository.save(new Property(
                    "Lakeview Annex",
                    "Studio",
                    "221 6th Block",
                    "Koramangala",
                    "Bengaluru",
                    "Karnataka",
                    "560095",
                    "Quiet side street near offices and cafes.",
                    fernandez,
                    LocalDate.of(2023, 9, 1),
                    karan
            ));
            koramangala.setMonthlyRent(java.math.BigDecimal.valueOf(22000));
            koramangala.setDepositAmount(java.math.BigDecimal.valueOf(44000));
            koramangala.setPropertyConditionOnEntry(PropertyCondition.EXCELLENT);
            koramangala.setPropertyConditionOnExit(PropertyCondition.EXCELLENT);
            koramangala.setExitDate(LocalDate.of(2024, 8, 31));
            koramangala.setFurnishingType(FurnishingType.FULLY_FURNISHED);
            koramangala.setOccupancyType(OccupancyType.SOLO);
            koramangala.setAmenities("WiFi,Parking,Pool,Furnished,AC");
            koramangala.setStatus(PropertyStatus.ACTIVE);
            propertyRepository.save(koramangala);

            Property andheri = propertyRepository.save(new Property(
                    "Palm Court",
                    "Apartment",
                    "9 Veera Desai Road",
                    "Andheri West",
                    "Mumbai",
                    "Maharashtra",
                    "400053",
                    "Lift worked well, frequent monsoon seepage on top floor units.",
                    patel,
                    LocalDate.of(2023, 3, 10),
                    meera
            ));
            andheri.setMonthlyRent(java.math.BigDecimal.valueOf(25000));
            andheri.setDepositAmount(java.math.BigDecimal.valueOf(50000));
            andheri.setPropertyConditionOnEntry(PropertyCondition.GOOD);
            andheri.setPropertyConditionOnExit(PropertyCondition.FAIR);
            andheri.setExitDate(LocalDate.of(2024, 9, 30));
            andheri.setFurnishingType(FurnishingType.SEMI_FURNISHED);
            andheri.setOccupancyType(OccupancyType.FAMILY);
            andheri.setAmenities("WiFi,Parking,Lift,AC,Balcony");
            andheri.setStatus(PropertyStatus.ACTIVE);
            propertyRepository.save(andheri);

            // Additional sample properties with different statuses
            // Property with PENDING_VERIFICATION status
            Property whitefieldPending = propertyRepository.save(new Property(
                    "Tech Park Residency",
                    "Apartment",
                    "Plot 456, Whitefield Road",
                    "Whitefield",
                    "Bengaluru",
                    "Karnataka",
                    "560066",
                    "Modern tech hub location, well-maintained complex.",
                    fernandez,
                    LocalDate.of(2024, 1, 20),
                    karan
            ));
            whitefieldPending.setMonthlyRent(java.math.BigDecimal.valueOf(28000));
            whitefieldPending.setDepositAmount(java.math.BigDecimal.valueOf(56000));
            whitefieldPending.setPropertyConditionOnEntry(PropertyCondition.EXCELLENT);
            whitefieldPending.setFurnishingType(FurnishingType.FULLY_FURNISHED);
            whitefieldPending.setOccupancyType(OccupancyType.SHARED);
            whitefieldPending.setAmenities("WiFi,Parking,Gym,Pool,Furnished,Co-working,Security");
            whitefieldPending.setStatus(PropertyStatus.PENDING_VERIFICATION);
            propertyRepository.save(whitefieldPending);

            // Property with ACTIVE status and complete lifecycle data
            Property marathahalliActive = propertyRepository.save(new Property(
                    "Silk Valley Apartments",
                    "Apartment",
                    "123 Marathahalli Main",
                    "Marathahalli",
                    "Bengaluru",
                    "Karnataka",
                    "560037",
                    "Spacious units, metro proximity, multiple dining options nearby.",
                    patel,
                    LocalDate.of(2023, 8, 5),
                    aisha
            ));
            marathahalliActive.setMonthlyRent(java.math.BigDecimal.valueOf(20000));
            marathahalliActive.setDepositAmount(java.math.BigDecimal.valueOf(40000));
            marathahalliActive.setPropertyConditionOnEntry(PropertyCondition.GOOD);
            marathahalliActive.setPropertyConditionOnExit(PropertyCondition.GOOD);
            marathahalliActive.setExitDate(LocalDate.of(2025, 7, 31));
            marathahalliActive.setFurnishingType(FurnishingType.SEMI_FURNISHED);
            marathahalliActive.setOccupancyType(OccupancyType.SOLO);
            marathahalliActive.setAmenities("WiFi,Parking,Laundry,Balcony,AC");
            marathahalliActive.setStatus(PropertyStatus.ACTIVE);
            propertyRepository.save(marathahalliActive);

            // Property with ARCHIVED status and exit data
            Property jayanagar = propertyRepository.save(new Property(
                    "Garden View Manor",
                    "Apartment",
                    "78 8th Block",
                    "Jayanagar",
                    "Bengaluru",
                    "Karnataka",
                    "560082",
                    "Green locality, established community, convenient shopping district.",
                    fernandez,
                    LocalDate.of(2022, 5, 15),
                    meera
            ));
            jayanagar.setMonthlyRent(java.math.BigDecimal.valueOf(18000));
            jayanagar.setDepositAmount(java.math.BigDecimal.valueOf(36000));
            jayanagar.setPropertyConditionOnEntry(PropertyCondition.EXCELLENT);
            jayanagar.setPropertyConditionOnExit(PropertyCondition.FAIR);
            jayanagar.setExitDate(LocalDate.of(2023, 11, 30));
            jayanagar.setFurnishingType(FurnishingType.UNFURNISHED);
            jayanagar.setOccupancyType(OccupancyType.FAMILY);
            jayanagar.setAmenities("Parking,Community Hall,Garden,Maintenance Staff");
            jayanagar.setStatus(PropertyStatus.ARCHIVED);
            jayanagar.setVerifiedAt(java.time.LocalDateTime.of(2022, 5, 20, 10, 30));
            jayanagar.setVerifiedBy(aisha);
            propertyRepository.save(jayanagar);

            Review review1 = reviewRepository.save(new Review(
                    indiranagar,
                    patel,
                    aisha,
                    "Responsive at first, difficult during deposit closure",
                    "The flat was bright and well located, but repeated plumbing leaks came back every few weeks.",
                    "Kitchen sink leakage, two weeks of water seepage in the utility area, and noisy diesel generator tests.",
                    "The landlord replied on WhatsApp quickly but usually sent the technician only after repeated follow-up.",
                    "Notice period was accepted without argument, but the final inspection added surprise cleaning charges.",
                    "One month of deposit was held for six weeks and partially reduced after escalation.",
                    3,
                    3,
                    4,
                    2,
                    3,
                    2,
                    false,
                    false,
                    false
            ));
            Review review2 = reviewRepository.save(new Review(
                    koramangala,
                    fernandez,
                    karan,
                    "Well run place with clear paperwork",
                    "Small studio, but the landlord kept the building tidy and fixed AC issues within two days.",
                    "Mostly normal wear issues and one internet outage caused by a vendor dispute.",
                    "Helpful, documented every visit, and shared receipts before deducting anything.",
                    "Renewal terms were sent early. Move-out checklist was strict but fair.",
                    "Deposit was returned in full within four business days.",
                    5,
                    5,
                    4,
                    5,
                    5,
                    5,
                    true,
                    true,
                    true
            ));
            Review review3 = reviewRepository.save(new Review(
                    andheri,
                    patel,
                    meera,
                    "Good access, but top-floor dampness is real",
                    "Commute was excellent, yet the bedroom wall developed damp patches every monsoon.",
                    "Recurring dampness, repainting without fixing the leak source, and lift downtime during rains.",
                    "The landlord discounted one month of maintenance but did not fully solve the root issue.",
                    "Exit was cordial. Renewal conversation focused on rent increase despite unresolved seepage.",
                    "Deposit came back after 45 days with a deduction for wall repainting.",
                    2,
                    2,
                    3,
                    2,
                    3,
                    2,
                    false,
                    false,
                    false
            ));

            ReviewComment comment1 = reviewCommentRepository.save(new ReviewComment(
                    review1,
                    karan,
                    null,
                    "Did the landlord eventually share invoices for the cleaning deduction?"
            ));
            reviewCommentRepository.save(new ReviewComment(
                    review1,
                    aisha,
                    comment1,
                    "Only after I pushed twice. The invoice existed, but it should have been shared during inspection."
            ));
            reviewCommentRepository.save(new ReviewComment(
                    review3,
                    aisha,
                    null,
                    "I saw the same dampness in a neighboring unit when I visited a friend there."
            ));

            reviewVoteRepository.save(new ReviewVote(review1, karan, ReviewVoteType.HELPFUL));
            reviewVoteRepository.save(new ReviewVote(review1, meera, ReviewVoteType.SAME_ISSUE));
            reviewVoteRepository.save(new ReviewVote(review2, aisha, ReviewVoteType.HELPFUL));
            reviewVoteRepository.save(new ReviewVote(review2, meera, ReviewVoteType.HELPFUL));
            reviewVoteRepository.save(new ReviewVote(review3, aisha, ReviewVoteType.HELPFUL));
            reviewVoteRepository.save(new ReviewVote(review3, karan, ReviewVoteType.SAME_ISSUE));
        };
    }
}

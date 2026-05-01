package com.rentwise.backend.config;

import com.rentwise.backend.auth.AuthProvider;
import com.rentwise.backend.landlord.Landlord;
import com.rentwise.backend.landlord.LandlordRepository;
import com.rentwise.backend.property.Property;
import com.rentwise.backend.property.PropertyRepository;
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

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedData(
            AppUserRepository userRepository,
            LandlordRepository landlordRepository,
            PropertyRepository propertyRepository,
            ReviewRepository reviewRepository,
            ReviewCommentRepository reviewCommentRepository,
            ReviewVoteRepository reviewVoteRepository
    ) {
        return args -> {
            if (propertyRepository.count() > 0) {
                return;
            }

            AppUser aisha = userRepository.save(new AppUser("Aisha", "aisha@example.com", null, AuthProvider.OTP_EMAIL, null));
            AppUser karan = userRepository.save(new AppUser("Karan", "karan@example.com", null, AuthProvider.GOOGLE, null));
            AppUser meera = userRepository.save(new AppUser("Meera", null, "+15550001001", AuthProvider.OTP_MOBILE, null));
            // New AppUser for landlords
            AppUser harishUser = userRepository.save(new AppUser("Harish Patel", "harish.patel@example.com", null, AuthProvider.OTP_EMAIL, null));
            AppUser rosaUser = userRepository.save(new AppUser("Rosa Fernandez", "rosa.fernandez@example.com", null, AuthProvider.OTP_EMAIL, null));

            Landlord patel = landlordRepository.save(new Landlord(
                    "Harish Patel",
                    "patel.rentals@example.example.com",
                    "+1-555-0100",
                    "Fast to lease, slower when maintenance affects only one tenant.",
                    harishUser
            ));
            Landlord fernandez = landlordRepository.save(new Landlord(
                    "Rosa Fernandez",
                    "rosa.fernandez.rentals@example.com",
                    "+1-555-0122",
                    "Professional documentation and clear move-out checklist.",
                    rosaUser
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
                    patel
            ));
            Property koramangala = propertyRepository.save(new Property(
                    "Lakeview Annex",
                    "Studio",
                    "221 6th Block",
                    "Koramangala",
                    "Bengaluru",
                    "Karnataka",
                    "560095",
                    "Quiet side street near offices and cafes.",
                    fernandez
            ));
            Property andheri = propertyRepository.save(new Property(
                    "Palm Court",
                    "Apartment",
                    "9 Veera Desai Road",
                    "Andheri West",
                    "Mumbai",
                    "Maharashtra",
                    "400053",
                    "Lift worked well, frequent monsoon seepage on top floor units.",
                    patel
            ));

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

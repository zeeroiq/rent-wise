package com.rentwise.backend.review;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewVoteRepository extends JpaRepository<ReviewVote, Long> {
    List<ReviewVote> findByReviewIdIn(List<Long> reviewIds);

    Optional<ReviewVote> findByReviewIdAndUserId(Long reviewId, Long userId);
}

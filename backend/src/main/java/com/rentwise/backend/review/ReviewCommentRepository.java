package com.rentwise.backend.review;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    List<ReviewComment> findByReviewIdInOrderByCreatedAtAsc(List<Long> reviewIds);
}

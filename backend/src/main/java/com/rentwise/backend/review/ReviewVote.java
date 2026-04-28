package com.rentwise.backend.review;

import com.rentwise.backend.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"review_id", "user_id"})
        }
)
public class ReviewVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewVoteType type;

    protected ReviewVote() {
    }

    public ReviewVote(Review review, AppUser user, ReviewVoteType type) {
        this.review = review;
        this.user = user;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public Review getReview() {
        return review;
    }

    public AppUser getUser() {
        return user;
    }

    public ReviewVoteType getType() {
        return type;
    }

    public void setType(ReviewVoteType type) {
        this.type = type;
    }
}

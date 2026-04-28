package com.rentwise.backend.review;

import com.rentwise.backend.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
public class ReviewComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private ReviewComment parent;

    @Column(nullable = false, length = 1_500)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected ReviewComment() {
    }

    public ReviewComment(Review review, AppUser author, ReviewComment parent, String body) {
        this.review = review;
        this.author = author;
        this.parent = parent;
        this.body = body;
    }

    public Long getId() {
        return id;
    }

    public Review getReview() {
        return review;
    }

    public AppUser getAuthor() {
        return author;
    }

    public ReviewComment getParent() {
        return parent;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

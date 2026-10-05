package com.agencyvoyage.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "hotel_review",
        uniqueConstraints = @UniqueConstraint(columnNames = {"hotel_id", "author_user_id"}))
public class HotelReviewJpaEntity {

    @Id
    private UUID id;

    @Column(name = "hotel_id", nullable = false)
    private UUID hotelId;

    @Column(name = "author_user_id", nullable = false)
    private UUID authorUserId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "reviewed_at", nullable = false)
    private Instant reviewedAt;

    protected HotelReviewJpaEntity() {
        // JPA
    }

    public HotelReviewJpaEntity(
            UUID id,
            UUID hotelId,
            UUID authorUserId,
            String authorName,
            int rating,
            String comment,
            Instant reviewedAt) {
        this.id = id;
        this.hotelId = hotelId;
        this.authorUserId = authorUserId;
        this.authorName = authorName;
        this.rating = rating;
        this.comment = comment;
        this.reviewedAt = reviewedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getHotelId() {
        return hotelId;
    }

    public UUID getAuthorUserId() {
        return authorUserId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}

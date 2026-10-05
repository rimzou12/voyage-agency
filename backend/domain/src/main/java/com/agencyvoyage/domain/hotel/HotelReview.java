package com.agencyvoyage.domain.hotel;

import com.agencyvoyage.domain.user.UserId;
import java.time.Instant;
import java.util.Objects;

/**
 * One traveler's rating (and optional comment) for a hotel, so other travelers can see
 * a score before picking where to stay. A user may have at most one review per hotel -
 * enforced at the application layer, not here, since checking for an existing review
 * needs a repository lookup.
 */
public record HotelReview(
        HotelReviewId id,
        HotelId hotelId,
        UserId authorId,
        String authorName,
        int rating,
        String comment,
        Instant reviewedAt) {

    public HotelReview {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(hotelId, "hotelId must not be null");
        Objects.requireNonNull(authorId, "authorId must not be null");
        Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
        if (authorName == null || authorName.isBlank()) {
            throw new IllegalArgumentException("authorName must not be blank");
        }
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5, got " + rating);
        }
        comment = comment == null || comment.isBlank() ? null : comment;
    }
}

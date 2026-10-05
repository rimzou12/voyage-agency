package com.agencyvoyage.web.dto;

import com.agencyvoyage.domain.hotel.HotelReview;
import java.time.Instant;

public record HotelReviewResponse(
        String id,
        String hotelId,
        String authorId,
        String authorName,
        int rating,
        String comment,
        Instant reviewedAt) {

    public static HotelReviewResponse from(HotelReview review) {
        return new HotelReviewResponse(
                review.id().toString(),
                review.hotelId().toString(),
                review.authorId().toString(),
                review.authorName(),
                review.rating(),
                review.comment(),
                review.reviewedAt());
    }
}

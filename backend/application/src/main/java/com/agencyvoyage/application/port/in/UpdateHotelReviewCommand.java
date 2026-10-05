package com.agencyvoyage.application.port.in;

import com.agencyvoyage.domain.hotel.HotelReviewId;
import com.agencyvoyage.domain.user.User;
import java.util.Objects;

public record UpdateHotelReviewCommand(HotelReviewId reviewId, int rating, String comment, User requestedBy) {

    public UpdateHotelReviewCommand {
        Objects.requireNonNull(reviewId, "reviewId must not be null");
        Objects.requireNonNull(requestedBy, "requestedBy must not be null");
    }
}

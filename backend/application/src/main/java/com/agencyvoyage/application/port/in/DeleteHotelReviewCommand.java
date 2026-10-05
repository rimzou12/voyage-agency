package com.agencyvoyage.application.port.in;

import com.agencyvoyage.domain.hotel.HotelReviewId;
import com.agencyvoyage.domain.user.User;
import java.util.Objects;

public record DeleteHotelReviewCommand(HotelReviewId reviewId, User requestedBy) {

    public DeleteHotelReviewCommand {
        Objects.requireNonNull(reviewId, "reviewId must not be null");
        Objects.requireNonNull(requestedBy, "requestedBy must not be null");
    }
}

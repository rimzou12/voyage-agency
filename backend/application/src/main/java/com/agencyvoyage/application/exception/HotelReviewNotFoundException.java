package com.agencyvoyage.application.exception;

import com.agencyvoyage.domain.hotel.HotelReviewId;

public final class HotelReviewNotFoundException extends RuntimeException {

    public HotelReviewNotFoundException(HotelReviewId id) {
        super("No review found with id " + id);
    }
}

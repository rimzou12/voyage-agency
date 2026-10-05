package com.agencyvoyage.application.port.in;

import com.agencyvoyage.domain.hotel.HotelReview;

/** Only the original author may edit their own review. */
public interface UpdateHotelReviewUseCase {

    HotelReview updateReview(UpdateHotelReviewCommand command);
}

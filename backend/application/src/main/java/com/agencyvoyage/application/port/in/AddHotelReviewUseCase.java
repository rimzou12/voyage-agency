package com.agencyvoyage.application.port.in;

import com.agencyvoyage.domain.hotel.HotelReview;

/** Any logged-in user may review a hotel, at most once each. */
public interface AddHotelReviewUseCase {

    HotelReview addReview(AddHotelReviewCommand command);
}

package com.agencyvoyage.application.port.in;

import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.hotel.HotelReview;
import java.util.List;

public interface ListHotelReviewsUseCase {

    /** Newest first. Public - no authentication required. */
    List<HotelReview> listReviews(HotelId hotelId);
}

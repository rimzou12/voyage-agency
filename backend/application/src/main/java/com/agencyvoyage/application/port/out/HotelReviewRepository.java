package com.agencyvoyage.application.port.out;

import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.hotel.HotelReview;
import com.agencyvoyage.domain.hotel.HotelReviewId;
import com.agencyvoyage.domain.user.UserId;
import java.util.List;
import java.util.Optional;

public interface HotelReviewRepository {

    void save(HotelReview review);

    Optional<HotelReview> findById(HotelReviewId id);

    /** Newest first. */
    List<HotelReview> findByHotelId(HotelId hotelId);

    Optional<HotelReview> findByHotelIdAndAuthorId(HotelId hotelId, UserId authorId);

    void deleteById(HotelReviewId id);
}

package com.agencyvoyage.application.service;

import com.agencyvoyage.application.port.in.ListHotelReviewsUseCase;
import com.agencyvoyage.application.port.out.HotelReviewRepository;
import com.agencyvoyage.domain.hotel.HotelId;
import com.agencyvoyage.domain.hotel.HotelReview;
import java.util.List;
import java.util.Objects;

public final class ListHotelReviewsService implements ListHotelReviewsUseCase {

    private final HotelReviewRepository hotelReviewRepository;

    public ListHotelReviewsService(HotelReviewRepository hotelReviewRepository) {
        this.hotelReviewRepository =
                Objects.requireNonNull(hotelReviewRepository, "hotelReviewRepository must not be null");
    }

    @Override
    public List<HotelReview> listReviews(HotelId hotelId) {
        return hotelReviewRepository.findByHotelId(hotelId);
    }
}

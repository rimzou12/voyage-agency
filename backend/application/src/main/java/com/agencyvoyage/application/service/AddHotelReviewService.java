package com.agencyvoyage.application.service;

import com.agencyvoyage.application.exception.AlreadyReviewedHotelException;
import com.agencyvoyage.application.port.in.AddHotelReviewCommand;
import com.agencyvoyage.application.port.in.AddHotelReviewUseCase;
import com.agencyvoyage.application.port.out.HotelReviewRepository;
import com.agencyvoyage.domain.hotel.HotelReview;
import com.agencyvoyage.domain.hotel.HotelReviewId;
import java.time.Clock;
import java.util.Objects;

public final class AddHotelReviewService implements AddHotelReviewUseCase {

    private final HotelReviewRepository hotelReviewRepository;
    private final Clock clock;

    public AddHotelReviewService(HotelReviewRepository hotelReviewRepository, Clock clock) {
        this.hotelReviewRepository =
                Objects.requireNonNull(hotelReviewRepository, "hotelReviewRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public HotelReview addReview(AddHotelReviewCommand command) {
        boolean alreadyReviewed = hotelReviewRepository
                .findByHotelIdAndAuthorId(command.hotelId(), command.author().id())
                .isPresent();
        if (alreadyReviewed) {
            throw new AlreadyReviewedHotelException();
        }

        HotelReview review = new HotelReview(
                HotelReviewId.newId(),
                command.hotelId(),
                command.author().id(),
                command.author().displayName(),
                command.rating(),
                command.comment(),
                clock.instant());

        hotelReviewRepository.save(review);
        return review;
    }
}

package com.agencyvoyage.application.service;

import com.agencyvoyage.application.exception.HotelReviewNotFoundException;
import com.agencyvoyage.application.exception.NotReviewAuthorException;
import com.agencyvoyage.application.port.in.UpdateHotelReviewCommand;
import com.agencyvoyage.application.port.in.UpdateHotelReviewUseCase;
import com.agencyvoyage.application.port.out.HotelReviewRepository;
import com.agencyvoyage.domain.hotel.HotelReview;
import java.time.Clock;
import java.util.Objects;

public final class UpdateHotelReviewService implements UpdateHotelReviewUseCase {

    private final HotelReviewRepository hotelReviewRepository;
    private final Clock clock;

    public UpdateHotelReviewService(HotelReviewRepository hotelReviewRepository, Clock clock) {
        this.hotelReviewRepository =
                Objects.requireNonNull(hotelReviewRepository, "hotelReviewRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public HotelReview updateReview(UpdateHotelReviewCommand command) {
        HotelReview existing = hotelReviewRepository
                .findById(command.reviewId())
                .orElseThrow(() -> new HotelReviewNotFoundException(command.reviewId()));

        if (!existing.authorId().equals(command.requestedBy().id())) {
            throw new NotReviewAuthorException();
        }

        HotelReview updated = new HotelReview(
                existing.id(),
                existing.hotelId(),
                existing.authorId(),
                existing.authorName(),
                command.rating(),
                command.comment(),
                clock.instant());

        hotelReviewRepository.save(updated);
        return updated;
    }
}

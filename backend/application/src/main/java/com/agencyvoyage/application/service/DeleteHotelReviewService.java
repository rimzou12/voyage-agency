package com.agencyvoyage.application.service;

import com.agencyvoyage.application.exception.HotelReviewNotFoundException;
import com.agencyvoyage.application.exception.NotReviewAuthorException;
import com.agencyvoyage.application.port.in.DeleteHotelReviewCommand;
import com.agencyvoyage.application.port.in.DeleteHotelReviewUseCase;
import com.agencyvoyage.application.port.out.HotelReviewRepository;
import com.agencyvoyage.domain.hotel.HotelReview;
import java.util.Objects;

public final class DeleteHotelReviewService implements DeleteHotelReviewUseCase {

    private final HotelReviewRepository hotelReviewRepository;

    public DeleteHotelReviewService(HotelReviewRepository hotelReviewRepository) {
        this.hotelReviewRepository =
                Objects.requireNonNull(hotelReviewRepository, "hotelReviewRepository must not be null");
    }

    @Override
    public void deleteReview(DeleteHotelReviewCommand command) {
        HotelReview existing = hotelReviewRepository
                .findById(command.reviewId())
                .orElseThrow(() -> new HotelReviewNotFoundException(command.reviewId()));

        boolean isAuthor = existing.authorId().equals(command.requestedBy().id());
        if (!isAuthor && !command.requestedBy().isAdmin()) {
            throw new NotReviewAuthorException();
        }

        hotelReviewRepository.deleteById(command.reviewId());
    }
}
